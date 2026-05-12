package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.RemplaceRequest;
import tn.esprit.rh_rse.entity.*;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RemplaceReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationNavetteRepository reservationNavetteRepository;
    private final TrajetRepository trajetRepository;
    private final BusRepository busRepository;
    private final EmpreinteCarboneService empreinteCarboneService;
    private final NotificationTransportService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public Reservation remplacerReservation(RemplaceRequest request) {

        // 1. Annuler ancienne réservation
        Reservation ancienne = reservationRepository
                .findById(request.getAncienneReservationId())
                .orElseThrow(() ->
                        new RuntimeException("Réservation introuvable"));
        ancienne.setStatut(StatutReservation.ANNULE);
        reservationRepository.save(ancienne);

        // 2. Créer nouvelle réservation
        Reservation nouvelle = new Reservation();
        nouvelle.setEmployeId(request.getEmployeId());
        nouvelle.setStatut(StatutReservation.EN_ATTENTE);
        nouvelle.setDateReservation(LocalDateTime.now());

        if ("COVOITURAGE".equals(request.getTypeAlternative())) {
            Trajet trajet = trajetRepository
                    .findById(request.getNouvelleAlternativeId())
                    .orElseThrow(() ->
                            new RuntimeException("Trajet introuvable"));

            if (trajet.getPlacesRestantes() <= 0)
                throw new RuntimeException(
                        "Plus de places disponibles");

            nouvelle.setTrajetId(trajet.getId());
            trajet.setPlacesRestantes(
                    trajet.getPlacesRestantes() - 1);
            trajetRepository.save(trajet);

        } else {
            Bus bus = busRepository
                    .findById(request.getNouvelleAlternativeId())
                    .orElseThrow(() ->
                            new RuntimeException("Bus introuvable"));

            if (bus.getPlacesRestantes() <= 0)
                throw new RuntimeException(
                        "Plus de places disponibles");

            ReservationNavette nouvelleNavette = ReservationNavette
                    .builder()
                    .busId(bus.getId())
                    .employeId(request.getEmployeId())
                    .dateReservation(LocalDateTime.now())
                    .statut(StatutReservation.EN_ATTENTE)
                    .ligne(bus.getLigne())
                    .heureDepart(bus.getHeureDepart())
                    .dureeMinutes(bus.getDureeMinutes())
                    .build();

            reservationNavetteRepository.save(nouvelleNavette);

            bus.setPlacesRestantes(bus.getPlacesRestantes() - 1);
            busRepository.save(bus);
        }

        Reservation saved = reservationRepository.save(nouvelle);

        // 3. Mise à jour points éco
        if ("COVOITURAGE".equals(request.getTypeAlternative())) {
            tn.esprit.rh_rse.entity.EmpreinteCarbone ec = new tn.esprit.rh_rse.entity.EmpreinteCarbone();
            ec.setEmployeId(request.getEmployeId());
            ec.setTrajetId(nouvelle.getTrajetId());
            ec.setRole(tn.esprit.rh_rse.entity.enums.RoleTrajet.PASSAGER);
            ec.setDistanceKm(20.0);
            ec.setTypeCarburant(tn.esprit.rh_rse.entity.enums.TypeCarburant.ESSENCE);
            ec.setNbPassagers(1);
            empreinteCarboneService.create(ec);
        }

        // 4. ✅ NOTIFICATION CORRIGÉE (utilise TypeNotification existant)
        String contenu = "Votre réservation a été remplacée avec succès par un " +
                request.getTypeAlternative().toLowerCase() + ".";

        notificationService.envoyerNotification(
                request.getEmployeId(),      // destinataireId
                "SYSTEME",                    // expediteurId
                null,                         // trajetId (pas applicable)
                TypeNotification.RESERVATION, // ← Utilise RESERVATION qui existe déjà
                contenu                       // contenu
        );

        return saved;
    }
}