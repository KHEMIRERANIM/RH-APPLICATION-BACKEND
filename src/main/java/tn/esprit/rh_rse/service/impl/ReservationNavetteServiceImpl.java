package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.ReservationNavetteRequest;
import tn.esprit.rh_rse.dto.response.ReservationNavetteResponse;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.entity.ReservationNavette;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.repository.BusRepository;
import tn.esprit.rh_rse.repository.ReservationNavetteRepository;
import tn.esprit.rh_rse.service.ReservationNavetteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationNavetteServiceImpl implements ReservationNavetteService {

    private final ReservationNavetteRepository reservationNavetteRepository;
    private final BusRepository busRepository;

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

        // Découper les jours sélectionnés
        String[] jours = request.getJoursSelectionnes().split(",");
        int nbJours = jours.length;

        // Vérifier les places disponibles pour chaque jour
        for (String jour : jours) {
            long reserved = reservationNavetteRepository.countByBusIdAndJoursSelectionnesContaining(
                    request.getBusId(), jour.trim());
            if (reserved >= bus.getCapacite()) {
                throw new RuntimeException("Plus de places disponibles pour le " + jour.trim());
            }
        }

        // Décrémenter les places pour chaque jour
        for (String jour : jours) {
            bus.setPlacesRestantes(bus.getPlacesRestantes() - 1);
        }

        if (bus.getPlacesRestantes() <= 0) {
            bus.setStatut(StatutTrajet.COMPLET);
        }
        busRepository.save(bus);

        // Calcul CO2 pour tous les jours
        double distanceKm = (bus.getDureeMinutes() * 40.0) / 60.0;
        double co2ParJour = distanceKm * 0.21;
        double co2Total = co2ParJour * nbJours;
        int pointsTotal = (int) (co2Total * 10);

        ReservationNavette reservation = ReservationNavette.builder()
                .busId(request.getBusId())
                .employeId(request.getEmployeId())
                .statut(StatutReservation.EN_ATTENTE)
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

        return toResponse(reservationNavetteRepository.save(reservation));
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
        Bus bus = busRepository.findById(reservation.getBusId())
                .orElseThrow(() -> new RuntimeException("Bus non trouvé"));

        String[] jours = reservation.getJoursSelectionnes().split(",");
        int nbJours = jours.length;

        bus.setPlacesRestantes(bus.getPlacesRestantes() + nbJours);
        if (bus.getStatut() == StatutTrajet.COMPLET && bus.getPlacesRestantes() > 0) {
            bus.setStatut(StatutTrajet.ACTIF);
        }
        busRepository.save(bus);

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