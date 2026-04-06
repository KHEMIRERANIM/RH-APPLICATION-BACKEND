package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.ReservationNavetteRequest;
import tn.esprit.rh_rse.dto.response.ReservationNavetteResponse;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.entity.ReservationNavette;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.BusRepository;
import tn.esprit.rh_rse.repository.ReservationNavetteRepository;
import tn.esprit.rh_rse.service.NotificationService;
import tn.esprit.rh_rse.service.ReservationNavetteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationNavetteServiceImpl implements ReservationNavetteService {

    private static final List<StatutReservation> STATUTS_OCCUPANT_PLACE = List.of(
            StatutReservation.CONFIRME,
            StatutReservation.EN_ATTENTE
    );

    private final ReservationNavetteRepository reservationNavetteRepository;
    private final BusRepository busRepository;
    private final NotificationService notificationService;

    private long compterPlacesOccupees(String busId, String jour) {
        return reservationNavetteRepository.countByBusIdAndJoursSelectionnesContainingAndStatutIn(
                busId, jour.trim(), STATUTS_OCCUPANT_PLACE);
    }

    private ReservationNavetteResponse toResponse(ReservationNavette r) {
        return ReservationNavetteResponse.builder()
                .id(r.getId())
                .busId(r.getBusId())
                .employeId(r.getEmployeId())
                .statut(r.getStatut())
                .dateReservation(r.getDateReservation())
                .ligne(r.getLigne())
                .heureDepart(r.getHeureDepart())
                .dureeMinutes(r.getDureeMinutes())
                .joursSelectionnes(r.getJoursSelectionnes())
                .pointsEco(r.getPointsEco())
                .co2EconomiseKg(r.getCo2EconomiseKg())
                .distanceKm(r.getDistanceKm())
                .build();
    }

    @Override
    public List<ReservationNavetteResponse> getAll() {
        return reservationNavetteRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationNavetteResponse getById(String id) {
        ReservationNavette r = reservationNavetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée : " + id));
        return toResponse(r);
    }

    @Override
    public ReservationNavetteResponse create(ReservationNavetteRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new RuntimeException("Bus non trouvé"));

        // ✅ 1 SEULE DATE par réservation
        String date = request.getJoursSelectionnes().trim();
        int nbJours = 1;

        // Vérification des doublons
        List<ReservationNavette> reservationsExistantes =
                reservationNavetteRepository.findByEmployeId(request.getEmployeId());

        for (ReservationNavette existante : reservationsExistantes) {
            if (existante.getStatut() == StatutReservation.ANNULE) continue;
            String[] joursExistants = existante.getJoursSelectionnes().split(",");
            for (String jourExistant : joursExistants) {
                if (jourExistant.trim().equals(date)) {
                    throw new RuntimeException("Vous avez déjà réservé pour le " + date);
                }
            }
        }

        // Vérifier places disponibles pour CETTE date
        long placesOccupees = reservationNavetteRepository
                .countByBusIdAndJoursSelectionnesContainingAndStatutIn(
                        request.getBusId(), date, STATUTS_OCCUPANT_PLACE);
        boolean isFull = placesOccupees >= bus.getCapacite();

        // Si bus INACTIF dans un pack
        if (bus.getStatut() == StatutTrajet.INACTIF && bus.getPackId() != null) {
            return creerReservationEnAttente(request, bus, nbJours);
        }

        if (isFull) {
            String packId = bus.getPackId();
            if (packId == null || packId.isBlank()) {
                throw new RuntimeException("Plus de places disponibles pour le " + date);
            }
            List<Bus> inactifs = busRepository.findByPackIdAndStatut(packId, StatutTrajet.INACTIF);
            if (inactifs.isEmpty()) {
                throw new RuntimeException("Plus de places disponibles. Aucun bus inactif disponible.");
            }
            return creerReservationEnAttente(request, bus, nbJours);
        }

        // ✅ Réservation normale - NE PAS toucher à placesRestantes du bus !
        // (supprimer les lignes qui modifient placesRestantes)

        double distanceKm = (bus.getDureeMinutes() * 40.0) / 60.0;
        double co2Total = distanceKm * 0.21;
        int pointsTotal = (int) (co2Total * 10);

        ReservationNavette reservation = ReservationNavette.builder()
                .busId(request.getBusId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.CONFIRME)
                .dateReservation(LocalDateTime.now())
                .ligne(bus.getLigne())
                .heureDepart(bus.getHeureDepart())
                .dureeMinutes(bus.getDureeMinutes())
                .joursSelectionnes(date)  // ← 1 seule date
                .distanceKm(distanceKm)
                .co2EconomiseKg(co2Total)
                .pointsEco(pointsTotal)
                .dateCalcul(LocalDate.now())
                .build();

        return toResponse(reservationNavetteRepository.save(reservation));
    }

        // ✅ Méthode utilitaire pour les réservations en attente
    private ReservationNavetteResponse creerReservationEnAttente(ReservationNavetteRequest request, Bus bus, int nbJours) {
        String packId = bus.getPackId();

        // Compter les réservations en attente avant
        List<Bus> busDuPack = busRepository.findByPackId(packId);
        List<String> idsPack = busDuPack.stream().map(Bus::getId).toList();
        long avantListeAttente = reservationNavetteRepository.countByBusIdInAndStatut(
                idsPack, StatutReservation.EN_ATTENTE_ACTIVATION);

        double distanceKm = (bus.getDureeMinutes() * 40.0) / 60.0;
        double co2ParJour = distanceKm * 0.21;
        double co2Total = co2ParJour * nbJours;
        int pointsTotal = (int) (co2Total * 10);

        ReservationNavette reservation = ReservationNavette.builder()
                .busId(request.getBusId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.EN_ATTENTE_ACTIVATION)
                .dateReservation(LocalDateTime.now())
                .ligne(bus.getLigne())
                .heureDepart(bus.getHeureDepart())
                .dureeMinutes(bus.getDureeMinutes())
                .joursSelectionnes(request.getJoursSelectionnes())
                .distanceKm(distanceKm)
                .co2EconomiseKg(co2Total)
                .pointsEco(pointsTotal)
                .dateCalcul(LocalDate.now())
                .build();

        reservationNavetteRepository.save(reservation);

        long apresListeAttente = avantListeAttente + 1;

        // Trouver le bus inactif pour connaître sa capacité
        Bus busInactif = busRepository.findByPackIdAndStatut(packId, StatutTrajet.INACTIF)
                .stream().findFirst().orElse(null);

        int capaciteReserve = (busInactif != null && busInactif.getCapacite() != null)
                ? busInactif.getCapacite() : 0;
        int seuil = Math.max(1, capaciteReserve / 2);

        // ✅ CAS 1 : Première personne en liste d'attente (début)
        if (avantListeAttente == 0 && apresListeAttente == 1) {
            notificationService.envoyerNotification(
                    "ADMIN",
                    "SYSTEM",
                    packId,
                    TypeNotification.ACTIVATION_BUS,
                    String.format(
                            "⚠️ Début de liste d'attente : 1 employé attend. Seuil d'activation à %d employés (moitié des %d places).",
                            seuil, capaciteReserve)
            );
        }

        // ✅ CAS 2 : Seuil atteint
        if (apresListeAttente >= seuil && avantListeAttente < seuil) {
            notificationService.envoyerNotification(
                    "ADMIN",
                    "SYSTEM",
                    packId,
                    TypeNotification.ACTIVATION_BUS,
                    String.format(
                            "🚨 ATTENTION ADMIN : %d employé(s) en attente ! Le seuil de %d (moitié des %d places) est atteint. Veuillez activer un bus de réserve immédiatement.",
                            apresListeAttente, seuil, capaciteReserve)
            );
        }

        return toResponse(reservation);
    }

    @Override
    public ReservationNavetteResponse update(String id, ReservationNavetteRequest request) {
        ReservationNavette existing = reservationNavetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        existing.setStatut(request.getStatut());
        return toResponse(reservationNavetteRepository.save(existing));
    }

    @Override
    public void delete(String id) {
        ReservationNavette reservation = reservationNavetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Restaurer les places dans le bus
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE_ACTIVATION) {
            Bus bus = busRepository.findById(reservation.getBusId())
                    .orElseThrow(() -> new RuntimeException("Bus non trouvé"));

            String[] jours = reservation.getJoursSelectionnes().split(",");
            int nbJours = jours.length;

            bus.setPlacesRestantes(bus.getPlacesRestantes() + nbJours);
            if (bus.getStatut() == StatutTrajet.COMPLET && bus.getPlacesRestantes() > 0) {
                bus.setStatut(StatutTrajet.ACTIF);
            }
            busRepository.save(bus);
        }

        reservationNavetteRepository.deleteById(id);
    }

    @Override
    public List<ReservationNavetteResponse> getByEmployeId(String employeId) {
        return reservationNavetteRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalPointsByEmploye(String employeId) {
        return reservationNavetteRepository.findByEmployeId(employeId).stream()
                .filter(r -> r.getStatut() == StatutReservation.CONFIRME)
                .mapToInt(r -> r.getPointsEco() != null ? r.getPointsEco() : 0)
                .sum();
    }
}
