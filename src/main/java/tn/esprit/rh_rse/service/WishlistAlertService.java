package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.Wishlist;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.NotificationRepository;
import tn.esprit.rh_rse.repository.OffreAvantageRepository;
import tn.esprit.rh_rse.repository.WishlistRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistAlertService {

    private final WishlistRepository wishlistRepository;
    private final OffreAvantageRepository offreAvantageRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 */2 * * * *")
    public void detecterEvolutionsOffres() {
        log.info("lancement du WishlistAlertService (recherche d'évolutions tarifaires et disponibilités)");

        List<Wishlist> allWishlists = wishlistRepository.findAll();

        for (Wishlist wishlist : allWishlists) {
            if (wishlist.getIdOffreAvantage() == null) continue;
            OffreAvantage offreAvantage = offreAvantageRepository.findById(wishlist.getIdOffreAvantage()).orElse(null);
            if (offreAvantage == null || "INACTIVE".equals(offreAvantage.getStatut())) {
                continue;
            }

            boolean aMettreAJour = false;

            Double prixActuel = getPrixActuel(offreAvantage);
            Double dernierPrix = wishlist.getDernierPrixConnu() != null ? wishlist.getDernierPrixConnu() : prixActuel;

            if (prixActuel < dernierPrix) {
                creerNotification(wishlist.getIdUser(), TypeNotification.PRIX_BAISSE, offreAvantage, 
                        "Le prix de l'offreAvantage " + offreAvantage.getTitre() + " a baissé ! Il est passé de " + dernierPrix + " à " + prixActuel + " DT.");
                wishlist.setDernierPrixConnu(prixActuel);
                aMettreAJour = true;
            } else if (prixActuel > dernierPrix) {
                 // Si le prix monte on met à jour sans notifier
                 wishlist.setDernierPrixConnu(prixActuel);
                 aMettreAJour = true;
            }

            // 2. Détection de places libérées
            Integer placesDispo = offreAvantage.getNbPlacesDispo() != null ? offreAvantage.getNbPlacesDispo() : 0;
            Integer dernieresPlaces = wishlist.getDernieresPlacesDispoConnues() != null ? wishlist.getDernieresPlacesDispoConnues() : placesDispo;

            if (dernieresPlaces == 0 && placesDispo > 0) {
                creerNotification(wishlist.getIdUser(), TypeNotification.PLACES_LIBEREES, offreAvantage,
                        "Bonne nouvelle ! Il y a de nouveau de la place (" + placesDispo + " places) pour " + offreAvantage.getTitre() + " !");
                wishlist.setDernieresPlacesDispoConnues(placesDispo);
                aMettreAJour = true;
            } else if (!placesDispo.equals(dernieresPlaces)) {
                wishlist.setDernieresPlacesDispoConnues(placesDispo);
                aMettreAJour = true;
            }

            // 3. Détection d'expiration (dans 7 jours)
            if (offreAvantage.getDateFin() != null) {
                long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), offreAvantage.getDateFin());
                if (joursRestants > 0 && joursRestants <= 7) {
                    // Pour ne pas spammer tous les jours, on pourrait vérifier si on n'a pas DÉJÀ envoyé une notification d'expiration pour cette offreAvantage.
                    // Option simple: on vérifie qu'on ne le fait qu'à 7, 3 et 1 jour restant.
                    if (joursRestants == 7 || joursRestants == 3 || joursRestants == 1) {
                        // Optimisation: vérifier qu'on n'a pas déjà notifié récement (mais le CRON à la journée suffit généralement.
                        // vu qu'on est en test à la minute, ça spammera si on ne vérifie pas l'existence récente).
                        if (!existeNotificationRecenteExpiration(wishlist.getIdUser(), offreAvantage.getId())) {
                            creerNotification(wishlist.getIdUser(), TypeNotification.EXPIRATION_PROCHE, offreAvantage,
                                "Attention, l'offreAvantage " + offreAvantage.getTitre() + " expire dans " + joursRestants + " jours !");
                        }
                    }
                }
            }

            if (aMettreAJour) {
                wishlistRepository.save(wishlist);
            }
        }
    }

    private Double getPrixActuel(OffreAvantage offreAvantage) {
        if (offreAvantage.getCategorie() == CategorieOffreAvantage.HOTEL && offreAvantage.getDetailsHotel() != null) {
            return offreAvantage.getDetailsHotel().getPrixAdulte() != null ? offreAvantage.getDetailsHotel().getPrixAdulte() : 0.0;
        }
        return offreAvantage.getPrixConvention() != null ? offreAvantage.getPrixConvention() : 0.0;
    }

    private void creerNotification(String idUser, TypeNotification type, OffreAvantage offreAvantage, String message) {
        Notification notification = Notification.builder()
                .idUser(idUser)
                .type(type)
                .idOffreAvantage(offreAvantage.getId())
                .titreOffreAvantage(offreAvantage.getTitre())
                .message(message)
                .build();
        notificationRepository.save(notification);
        log.info("Notification créée pour user {} : {}", idUser, message);
    }

    private boolean existeNotificationRecenteExpiration(String idUser, String idOffreAvantage) {
        // En vrai, il faudrait utiliser la DB pour filtrer par type et date (moins de 24h par ex)
        // Mais pr le test: on récup non lu et on regarde
        return notificationRepository.findByIdUserOrderByDateCreationDesc(idUser).stream()
                .anyMatch(n -> idOffreAvantage.equals(n.getIdOffreAvantage()) && n.getType() == TypeNotification.EXPIRATION_PROCHE && !n.isLu());
    }
}

