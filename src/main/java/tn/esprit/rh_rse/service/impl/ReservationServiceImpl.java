package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.ReservationRequest;
import tn.esprit.rh_rse.dto.response.ReservationResponse;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.ReservationRepository;
import tn.esprit.rh_rse.repository.TrajetRepository;
import tn.esprit.rh_rse.service.NotificationService;
import tn.esprit.rh_rse.service.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;
    private final NotificationService notificationService; // ← ajouté

    // ─── MAPPER Entity → Response ──────────────────────────
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
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée : " + id));
        return toResponse(r);
    }

    @Override
    public ReservationResponse create(ReservationRequest request) {
        // Vérifier que le trajet existe
        Trajet trajet = trajetRepository.findById(request.getTrajetId())
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé"));

        // Vérifier les places disponibles
        if (trajet.getPlacesRestantes() <= 0) {
            throw new RuntimeException("Plus de places disponibles");
        }

        // Décrémenter les places
        trajet.setPlacesRestantes(trajet.getPlacesRestantes() - 1);
        if (trajet.getPlacesRestantes() == 0) {
            trajet.setStatut(StatutTrajet.COMPLET);
        }
        trajetRepository.save(trajet);

        // Calcul CO2 automatique
        double distanceKm   = 25.0;
        double co2Solo      = distanceKm * 0.21;
        double co2Covoit    = distanceKm * 0.05;
        double co2Economise = co2Solo - co2Covoit;
        int points          = (int) (co2Economise * 10);

        // Mapper Request → Entity
        Reservation reservation = Reservation.builder()
                .trajetId(request.getTrajetId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.EN_ATTENTE)
                .dateReservation(LocalDateTime.now())
                .co2AvecCovoit(co2Covoit)
                .co2EconomiseKg(co2Economise)
                .pointsEco(points)
                .dateCalcul(LocalDate.now())
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // ─── Notifications automatiques ────────────────────

        // 1. Notifier le passager
        notificationService.envoyerNotification(
                request.getEmployeId(),
                "SYSTEME",
                request.getTrajetId(),
                TypeNotification.RESERVATION,
                "Votre demande de réservation est en attente"
        );

        // 2. Notifier le conducteur pour confirmation
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
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée : " + id));

        // Si annulation → remettre la place dans le trajet
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

        // ─── Notifications selon statut ────────────────────
        if (request.getStatut() == StatutReservation.CONFIRME) {
            notificationService.envoyerNotification(
                    existing.getEmployeId(),
                    "SYSTEME",
                    existing.getTrajetId(),
                    TypeNotification.RESERVATION,
                    "Votre réservation a été confirmée ✅"
            );
        } else if (request.getStatut() == StatutReservation.ANNULE) {
            notificationService.envoyerNotification(
                    existing.getEmployeId(),
                    "SYSTEME",
                    existing.getTrajetId(),
                    TypeNotification.RESERVATION,
                    "Votre réservation a été annulée ❌"
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
                .filter(r -> r.getStatut() == StatutReservation.CONFIRME)
                .mapToInt(r -> r.getPointsEco() != null ? r.getPointsEco() : 0)
                .sum();
    }
}