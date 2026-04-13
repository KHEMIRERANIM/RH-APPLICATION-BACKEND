package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.dto.request.DemandeRemplacementCovoiturageRequest;
import tn.esprit.rh_rse.dto.request.ReservationRequest;
import tn.esprit.rh_rse.dto.response.ReservationResponse;
import tn.esprit.rh_rse.entity.EmpreinteCarbone;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.RoleTrajet;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.entity.enums.TypeCarburant;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.ReservationRepository;
import tn.esprit.rh_rse.repository.TrajetRepository;
import tn.esprit.rh_rse.repository.VehiculeRepository;
import tn.esprit.rh_rse.service.EmpreinteCarboneService;
import tn.esprit.rh_rse.service.NotificationService;
import tn.esprit.rh_rse.service.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import tn.esprit.rh_rse.entity.Vehicule;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;
    private final VehiculeRepository vehiculeRepository;
    private final EmpreinteCarboneService empreinteCarboneService;
    private final NotificationService notificationService;

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .trajetId(r.getTrajetId())
                .employeId(r.getEmployeId())
                .statut(r.getStatut())
                .dateReservation(r.getDateReservation())
                .co2AvecCovoit(r.getCo2AvecCovoit())
                .co2EconomiseKg(r.getCo2EconomiseKg())
                .pointsEco(r.getPointsEco())
                .dateCalcul(r.getDateCalcul())
                .remplaceReservationId(r.getRemplaceReservationId())
                .build();
    }

    @Override
    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationResponse getById(String id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation introuvable : " + id));
        return toResponse(r);
    }

    @Override
    public ReservationResponse create(ReservationRequest request) {
        Trajet trajet = trajetRepository.findById(request.getTrajetId())
                .orElseThrow(() -> new RuntimeException("Trajet introuvable"));

        System.out.println("=== DISTANCE RECUE : " + request.getDistanceKm());

        if (trajet.getPlacesRestantes() <= 0) {
            throw new RuntimeException("Plus de places disponibles");
        }

        trajet.setPlacesRestantes(trajet.getPlacesRestantes() - 1);
        if (trajet.getPlacesRestantes() == 0) {
            trajet.setStatut(StatutTrajet.COMPLET);
        }
        trajetRepository.save(trajet);

        double distanceKm   = request.getDistanceKm() != null ? request.getDistanceKm() : 25.0;
        double co2Solo      = distanceKm * 0.21;
        double co2Covoit    = distanceKm * 0.05;
        double co2Economise = co2Solo - co2Covoit;
        int points          = (int) (co2Economise * 10);

        Reservation reservation = Reservation.builder()
                .trajetId(request.getTrajetId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.EN_ATTENTE)
                .dateReservation(LocalDateTime.now())
                .co2AvecCovoit(co2Covoit)
                .co2EconomiseKg(co2Economise)
                .pointsEco(points)
                .dateCalcul(LocalDate.now())
                .distanceKm(distanceKm) // ← ajouter

                .build();


        Reservation saved = reservationRepository.save(reservation);

        notificationService.envoyerNotification(
                request.getEmployeId(),
                "SYSTEME",
                request.getTrajetId(),
                TypeNotification.RESERVATION,
                "Votre demande de reservation est en attente"
        );

        notificationService.envoyerDemandeConfirmation(
                trajet.getEmployeId(),
                request.getEmployeId(),
                request.getTrajetId(),
                saved.getId()
        );

        return toResponse(saved);
    }

    @Override
    public ReservationResponse update(String id, ReservationRequest request) {
        Reservation existing = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation introuvable : " + id));

        // Annulation : remettre la place
        if (request.getStatut() == StatutReservation.ANNULE
                && existing.getStatut() != StatutReservation.ANNULE) {
            trajetRepository.findById(existing.getTrajetId()).ifPresent(trajet -> {
                trajet.setPlacesRestantes(trajet.getPlacesRestantes() + 1);
                if (trajet.getStatut() == StatutTrajet.COMPLET) {
                    trajet.setStatut(StatutTrajet.ACTIF);
                }
                trajetRepository.save(trajet);
            });
        }

        // Acceptation : début du délai de paiement (15 min)
        if (request.getStatut() == StatutReservation.EN_ATTENTE_PAIEMENT
                && existing.getStatut() != StatutReservation.EN_ATTENTE_PAIEMENT) {
            existing.setDateAcceptation(LocalDateTime.now());
        }

        // Confirmation : creer les empreintes carbone
        if (request.getStatut() == StatutReservation.CONFIRME
                && existing.getStatut() != StatutReservation.CONFIRME) {

            trajetRepository.findById(existing.getTrajetId()).ifPresent(trajet -> {

                // APRES
                TypeCarburant typeCarburant = vehiculeRepository
                        .findById(trajet.getVehiculeId() != null ? trajet.getVehiculeId() : "")
                        .map(Vehicule::getTypeCarburant)

                        .orElse(TypeCarburant.ESSENCE);

                int nbPassagers = Math.max(1,
                        trajet.getPlacesDisponibles() - trajet.getPlacesRestantes());

                double distanceKm = (existing.getDistanceKm() != null && existing.getDistanceKm() > 0)
                        ? existing.getDistanceKm()
                        : 25.0;



                empreinteCarboneService.create(
                        EmpreinteCarbone.builder()
                                .employeId(existing.getEmployeId())
                                .trajetId(existing.getTrajetId())
                                .role(RoleTrajet.PASSAGER)
                                .distanceKm(distanceKm)
                                .nbPassagers(nbPassagers)
                                .typeCarburant(typeCarburant)
                                .build()
                );

                empreinteCarboneService.create(
                        EmpreinteCarbone.builder()
                                .employeId(trajet.getEmployeId())
                                .trajetId(trajet.getId())
                                .role(RoleTrajet.CONDUCTEUR)
                                .distanceKm(distanceKm)
                                .nbPassagers(nbPassagers)
                                .typeCarburant(typeCarburant)
                                .build()
                );
            });

            // Après confirmation : annuler la réservation covoiturage remplacée (alternatives)
            if (existing.getRemplaceReservationId() != null && !existing.getRemplaceReservationId().isBlank()) {
                annulerReservationPourRemplacement(existing.getRemplaceReservationId());
            }
        }

        if (request.getStatut() == StatutReservation.CONFIRME) {
            notificationService.envoyerNotification(
                    existing.getEmployeId(),
                    "SYSTEME",
                    existing.getTrajetId(),
                    TypeNotification.RESERVATION,
                    "Votre reservation a ete confirmee"
            );
        } else if (request.getStatut() == StatutReservation.ANNULE) {
            notificationService.envoyerNotification(
                    existing.getEmployeId(),
                    "SYSTEME",
                    existing.getTrajetId(),
                    TypeNotification.RESERVATION,
                    "Votre reservation a ete annulee"
            );
        }

        existing.setStatut(request.getStatut());
        return toResponse(reservationRepository.save(existing));
    }

    @Override
    public void delete(String id) {
        reservationRepository.deleteById(id);
    }

    @Override
    public List<ReservationResponse> getByEmployeId(String employeId) {
        return reservationRepository.findByEmployeId(employeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getByTrajetId(String trajetId) {
        return reservationRepository.findByTrajetId(trajetId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getByStatut(StatutReservation statut) {
        return reservationRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalPointsEcoByEmploye(String employeId) {
        return reservationRepository.findByEmployeId(employeId)
                .stream()
                .filter(r -> r.getStatut() != StatutReservation.ANNULE)
                .mapToInt(r -> r.getPointsEco() != null ? r.getPointsEco() : 0)
                .sum();
    }

    /**
     * Annule l'ancienne réservation (passager) sans repasser par update() pour éviter les boucles.
     */
    private void annulerReservationPourRemplacement(String ancienneReservationId) {
        Reservation ancienne = reservationRepository.findById(ancienneReservationId).orElse(null);
        if (ancienne == null || ancienne.getStatut() == StatutReservation.ANNULE) {
            return;
        }
        trajetRepository.findById(ancienne.getTrajetId()).ifPresent(trajet -> {
            trajet.setPlacesRestantes(trajet.getPlacesRestantes() + 1);
            if (trajet.getStatut() == StatutTrajet.COMPLET) {
                trajet.setStatut(StatutTrajet.ACTIF);
            }
            trajetRepository.save(trajet);
        });
        ancienne.setStatut(StatutReservation.ANNULE);
        reservationRepository.save(ancienne);
        notificationService.envoyerNotification(
                ancienne.getEmployeId(),
                "SYSTEME",
                ancienne.getTrajetId(),
                TypeNotification.RESERVATION,
                "Votre ancienne reservation a ete remplacee par la nouvelle."
        );
    }

    @Override
    public ReservationResponse demanderRemplacementCovoiturage(DemandeRemplacementCovoiturageRequest request) {
        Reservation ancienne = reservationRepository.findById(request.getAncienneReservationId())
                .orElseThrow(() -> new RuntimeException("Ancienne reservation introuvable"));
        if (!ancienne.getEmployeId().equals(request.getEmployeId())) {
            throw new RuntimeException("Reservation ne correspond pas a l'employe");
        }
        if (ancienne.getStatut() == StatutReservation.ANNULE) {
            throw new RuntimeException("Ancienne reservation deja annulee");
        }
        Trajet trajet = trajetRepository.findById(request.getNouveauTrajetId())
                .orElseThrow(() -> new RuntimeException("Trajet introuvable"));
        if (trajet.getPlacesRestantes() <= 0) {
            throw new RuntimeException("Plus de places disponibles");
        }
        trajet.setPlacesRestantes(trajet.getPlacesRestantes() - 1);
        if (trajet.getPlacesRestantes() == 0) {
            trajet.setStatut(StatutTrajet.COMPLET);
        }
        trajetRepository.save(trajet);

        double distanceKm = request.getDistanceKm() != null ? request.getDistanceKm() : 25.0;
        double co2Solo = distanceKm * 0.21;
        double co2Covoit = distanceKm * 0.05;
        double co2Economise = co2Solo - co2Covoit;
        int points = (int) (co2Economise * 10);

        Reservation reservation = Reservation.builder()
                .trajetId(request.getNouveauTrajetId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.EN_ATTENTE)
                .dateReservation(LocalDateTime.now())
                .co2AvecCovoit(co2Covoit)
                .co2EconomiseKg(co2Economise)
                .pointsEco(points)
                .dateCalcul(LocalDate.now())
                .distanceKm(distanceKm)
                .remplaceReservationId(request.getAncienneReservationId())
                .build();

        Reservation saved = reservationRepository.save(reservation);

        notificationService.envoyerNotification(
                request.getEmployeId(),
                "SYSTEME",
                request.getNouveauTrajetId(),
                TypeNotification.RESERVATION,
                "Demande de remplacement apres annulation — en attente du conducteur"
        );

        notificationService.envoyerDemandeConfirmation(
                trajet.getEmployeId(),
                request.getEmployeId(),
                request.getNouveauTrajetId(),
                saved.getId()
        );

        return toResponse(saved);
    }
    @Override
    @Transactional
    public void confirmerReservationApresPaiement(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)  // ← Plus de Long.valueOf
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée : " + reservationId));
        reservation.setStatut(StatutReservation.CONFIRME);  // ← Enum au lieu de String
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void updateStatusByTrajetId(String trajetId, StatutReservation statut) {
        List<Reservation> reservations = reservationRepository.findByTrajetId(trajetId);
        for (Reservation r : reservations) {
            r.setStatut(statut);
        }
        reservationRepository.saveAll(reservations);
    }

}