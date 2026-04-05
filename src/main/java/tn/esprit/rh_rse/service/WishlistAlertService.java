package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.Wishlist;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.NotificationRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.WishlistRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistAlertService {

    private final WishlistRepository wishlistRepository;
    private final OffreRepository offreRepository;
    private final NotificationRepository notificationRepository;

    // S'exécute toutes les 2 minutes pour les tests. À remettre à "0 0 8 * * *" (8h matin) pour prod.
    @Scheduled(cron = "0 */2 * * * *")
    public void detecterEvolutionsOffres() {
        log.info("Lancement du WishlistAlertService (recherche d'évolutions tarifaires et disponibilités)");

        List<Wishlist> allWishlists = wishlistRepository.findAll();

        for (Wishlist wishlist : allWishlists) {
            Offre offre = offreRepository.findById(wishlist.getIdOffre()).orElse(null);
            if (offre == null || "INACTIVE".equals(offre.getStatut())) {
                continue;
            }

            boolean aMettreAJour = false;

            // 1. Détection de baisse de prix
            Double prixActuel = getPrixActuel(offre);
            Double dernierPrix = wishlist.getDernierPrixConnu() != null ? wishlist.getDernierPrixConnu() : prixActuel;

            if (prixActuel < dernierPrix) {
                creerNotification(wishlist.getIdUser(), TypeNotification.PRIX_BAISSE, offre, 
                        "Le prix de l'offre " + offre.getTitre() + " a baissé ! Il est passé de " + dernierPrix + " à " + prixActuel + " DT.");
                wishlist.setDernierPrixConnu(prixActuel);
                aMettreAJour = true;
            } else if (prixActuel > dernierPrix) {
                 // Si le prix monte on met à jour sans notifier
                 wishlist.setDernierPrixConnu(prixActuel);
                 aMettreAJour = true;
            }

            // 2. Détection de places libérées
            Integer placesDispo = offre.getNbPlacesDispo() != null ? offre.getNbPlacesDispo() : 0;
            Integer dernieresPlaces = wishlist.getDernieresPlacesDispoConnues() != null ? wishlist.getDernieresPlacesDispoConnues() : placesDispo;

            if (dernieresPlaces == 0 && placesDispo > 0) {
                creerNotification(wishlist.getIdUser(), TypeNotification.PLACES_LIBEREES, offre,
                        "Bonne nouvelle ! Il y a de nouveau de la place (" + placesDispo + " places) pour " + offre.getTitre() + " !");
                wishlist.setDernieresPlacesDispoConnues(placesDispo);
                aMettreAJour = true;
            } else if (!placesDispo.equals(dernieresPlaces)) {
                wishlist.setDernieresPlacesDispoConnues(placesDispo);
                aMettreAJour = true;
            }

            // 3. Détection d'expiration (dans 7 jours)
            if (offre.getDateFin() != null) {
                long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), offre.getDateFin());
                if (joursRestants > 0 && joursRestants <= 7) {
                    // Pour ne pas spammer tous les jours, on pourrait vérifier si on n'a pas DÉJÀ envoyé une notification d'expiration pour cette offre.
                    // Option simple: on vérifie qu'on ne le fait qu'à 7, 3 et 1 jour restant.
                    if (joursRestants == 7 || joursRestants == 3 || joursRestants == 1) {
                        // Optimisation: vérifier qu'on n'a pas déjà notifié récement (mais le CRON à la journée suffit généralement.
                        // vu qu'on est en test à la minute, ça spammera si on ne vérifie pas l'existence récente).
                        if (!existeNotificationRecenteExpiration(wishlist.getIdUser(), offre.getId())) {
                            creerNotification(wishlist.getIdUser(), TypeNotification.EXPIRATION_PROCHE, offre,
                                "Attention, l'offre " + offre.getTitre() + " expire dans " + joursRestants + " jours !");
                        }
                    }
                }
            }

            if (aMettreAJour) {
                wishlistRepository.save(wishlist);
            }
        }
    }

    private Double getPrixActuel(Offre offre) {
        if (offre.getCategorie() == CategorieOffre.HOTEL && offre.getDetailsHotel() != null) {
            return offre.getDetailsHotel().getPrixAdulte() != null ? offre.getDetailsHotel().getPrixAdulte() : 0.0;
        }
        return offre.getPrixConvention() != null ? offre.getPrixConvention() : 0.0;
    }

    private void creerNotification(String idUser, TypeNotification type, Offre offre, String message) {
        Notification notification = Notification.builder()
                .idUser(idUser)
                .type(type)
                .idOffre(offre.getId())
                .titreOffre(offre.getTitre())
                .message(message)
                .build();
        notificationRepository.save(notification);
        log.info("Notification créée pour user {} : {}", idUser, message);
    }

    private boolean existeNotificationRecenteExpiration(String idUser, String idOffre) {
        // En vrai, il faudrait utiliser la DB pour filtrer par type et date (moins de 24h par ex)
        // Mais pr le test: on récup non lu et on regarde
        return notificationRepository.findByIdUserOrderByDateCreationDesc(idUser).stream()
                .anyMatch(n -> n.getIdOffre().equals(idOffre) && n.getType() == TypeNotification.EXPIRATION_PROCHE && !n.isLu());
    }
}
