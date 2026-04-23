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
                .joursConfirmes(r.getJoursConfirmes())
                .joursEnAttente(r.getJoursEnAttente())
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

        String packId = bus.getPackId();
        String[] jours = request.getJoursSelectionnes().split(",");
        List<String> joursConfirmes = new java.util.ArrayList<>();
        List<String> joursEnAttente = new java.util.ArrayList<>();

        // Vérification des doublons
        List<ReservationNavette> reservationsExistantes =
                reservationNavetteRepository.findByEmployeId(request.getEmployeId());

        for (String jour : jours) {
            String j = jour.trim();
            for (ReservationNavette existante : reservationsExistantes) {
                if (existante.getStatut() == StatutReservation.ANNULE) continue;
                if (existante.getJoursSelectionnes().contains(j)) {
                    throw new RuntimeException("Vous avez déjà réservé pour le " + j);
                }
            }

            // Calcul de la capacité au niveau du pack pour ce jour
            long placesOccupees = reservationNavetteRepository
                    .countByBusIdAndJoursSelectionnesContainingAndStatutIn(
                            request.getBusId(), j, STATUTS_OCCUPANT_PLACE);

            boolean busInactif = bus.getStatut() == StatutTrajet.INACTIF;
            boolean isFull = placesOccupees >= bus.getCapacite();

            if (busInactif && packId != null) {
                joursEnAttente.add(j);
            } else if (isFull && packId != null) {
                Bus busInactifDuPack = busRepository.findByPackId(packId).stream()
                        .filter(b -> b.getStatut() == StatutTrajet.INACTIF)
                        .findFirst().orElse(null);
                if (busInactifDuPack != null) {
                    request.setBusId(busInactifDuPack.getId());
                    bus = busInactifDuPack;
                    packId = busInactifDuPack.getPackId();
                }
                joursEnAttente.add(j);
            } else if (isFull && packId == null) {
                throw new RuntimeException("Plus de places disponibles pour le " + j);
            } else {
                joursConfirmes.add(j);
            }
// Un jour ne peut jamais être dans les deux listes
        }

        double distanceKm = (bus.getDureeMinutes() * 40.0) / 60.0;
        double co2Total = distanceKm * 0.21 * jours.length;
        int pointsTotal = (int) (co2Total * 10);

        StatutReservation statutGlobal = joursEnAttente.isEmpty() ? StatutReservation.CONFIRME : StatutReservation.EN_ATTENTE_ACTIVATION;

        ReservationNavette reservation = ReservationNavette.builder()
                .busId(request.getBusId())
                .employeId(request.getEmployeId())
                .statut(statutGlobal)
                .dateReservation(LocalDateTime.now())
                .ligne(bus.getLigne())
                .heureDepart(bus.getHeureDepart())
                .dureeMinutes(bus.getDureeMinutes())
                .joursSelectionnes(request.getJoursSelectionnes())
                .joursConfirmes(joursConfirmes)
                .joursEnAttente(joursEnAttente)
                .distanceKm(distanceKm)
                .co2EconomiseKg(co2Total)
                .pointsEco(pointsTotal)
                .dateCalcul(LocalDate.now())
                .build();

        reservationNavetteRepository.save(reservation);

        // Notifications Admin si au moins un jour est en attente
        if (!joursEnAttente.isEmpty() && packId != null) {
            envoyerNotificationsAdminSiBesoin(packId);
        }

        return toResponse(reservation);
    }

    private void envoyerNotificationsAdminSiBesoin(String packId) {
        List<Bus> busDuPack = busRepository.findByPackId(packId);
        List<String> idsPack = busDuPack.stream().map(Bus::getId).toList();
        
        Bus busInactif = busDuPack.stream()
                .filter(b -> b.getStatut() == StatutTrajet.INACTIF)
                .findFirst().orElse(null);
        
        int capaciteReserve = (busInactif != null && busInactif.getCapacite() != null) ? busInactif.getCapacite() : 15;
        int seuil = Math.max(1, capaciteReserve / 2);

        // 1. Récupérer toutes les réservations EN_ATTENTE_ACTIVATION du pack
        List<ReservationNavette> allWaiting = reservationNavetteRepository.findByBusIdInAndStatut(
                idsPack, StatutReservation.EN_ATTENTE_ACTIVATION);

        // 2. Extraire tous les jours uniques présents dans ces réservations
        java.util.Set<String> joursEnAttente = allWaiting.stream()
                .flatMap(r -> r.getJoursEnAttente().stream())
                .collect(Collectors.toSet());

        // 3. Pour chaque jour, compter les passagers et alerter si seuil atteint
        for (String jour : joursEnAttente) {
            long countPourCeJour = allWaiting.stream()
                    .filter(r -> r.getJoursEnAttente().contains(jour))
                    .count();

            if (countPourCeJour >= seuil) {
                notificationService.envoyerNotification(
                        "ADMIN", "SYSTEM", packId, TypeNotification.ACTIVATION_BUS,
                        String.format("🚨 SEUIL PAR JOUR ATTEINT : Le jour [%s] a %d passagers en attente sur le pack %s. Seuil de %d atteint (Moitié des %d places).", jour, countPourCeJour, packId, seuil, capaciteReserve)
                );
            }
        }

        // Notification globale (optionnelle si un passager arrive)
        long totalEnAttente = allWaiting.size();
        if (totalEnAttente == 1) {
            notificationService.envoyerNotification(
                    "ADMIN", "SYSTEM", packId, TypeNotification.ACTIVATION_BUS,
                    String.format("⚠️ Début de liste d'attente : Premier employé en attente sur le pack %s.", packId)
            );
        }
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
