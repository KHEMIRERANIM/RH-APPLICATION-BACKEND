package tn.esprit.rh_rse.service.impl;
import tn.esprit.rh_rse.entity.ReservationNavette;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.ReservationNavetteRepository;
import tn.esprit.rh_rse.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.BusPackRequest;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.repository.BusRepository;
import tn.esprit.rh_rse.service.BusService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final ReservationNavetteRepository reservationNavetteRepository;
    private final NotificationService notificationService;

    @Override
    public List<Bus> getAll() {
        return busRepository.findAll();
    }

    @Override
    public Bus getById(String id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus non trouvé : " + id));
    }

    @Override
    public Bus create(Bus bus) {
        if (bus.getStatut() == null) {
            bus.setStatut(StatutTrajet.ACTIF);
        }
        if (bus.getPlacesRestantes() == null || bus.getPlacesRestantes() == 0) {
            bus.setPlacesRestantes(bus.getCapacite());
        }



        bus.setDateCreation(LocalDate.now());
        return busRepository.save(bus);
    }

    @Override
    public List<Bus> createPack(BusPackRequest request) {
        if (request.getBusData() == null) {
            throw new RuntimeException("busData est requis");
        }
        if (request.getActiveCount() < 1) {
            throw new RuntimeException("activeCount doit être au moins 1");
        }
        if (request.getInactiveCount() < 0) {
            throw new RuntimeException("inactiveCount ne peut pas être négatif");
        }
        Bus template = request.getBusData();
        String packId = UUID.randomUUID().toString();
        String baseImmat = template.getImmatriculation() != null ? template.getImmatriculation().trim() : "BUS";
        int total = request.getActiveCount() + request.getInactiveCount();
        List<Bus> created = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < total; i++) {
            boolean isActive = i < request.getActiveCount();

            // Capacité individuelle si fournie, sinon capacité du template
            int capacite = (request.getBusCapacities() != null && i < request.getBusCapacities().size())
                    ? request.getBusCapacities().get(i)
                    : template.getCapacite();

            Bus b = Bus.builder()
                    .marque(template.getMarque())
                    .modele(template.getModele())
                    .immatriculation(total > 1 ? baseImmat + "-" + (i + 1) : baseImmat)
                    .capacite(capacite)                  // ← capacité individuelle
                    .typeCarburant(template.getTypeCarburant())
                    .ligne(template.getLigne())
                    .heureDepart(template.getHeureDepart())
                    .dureeMinutes(template.getDureeMinutes())
                    .joursDisponibles(template.getJoursDisponibles())
                    .placesRestantes(capacite)            // ← idem
                    .statut(isActive ? StatutTrajet.ACTIF : StatutTrajet.INACTIF)
                    .dateCreation(today)
                    .photoUrl(template.getPhotoUrl())
                    .packId(packId)
                    .build();
            created.add(busRepository.save(b));
        }
        return created;
    }

    @Override
    public Bus update(String id, Bus busUpdates) {
        Bus existing = getById(id);

        // Vérifier si le statut change de INACTIF à ACTIF
        boolean wasInactif = existing.getStatut() == StatutTrajet.INACTIF;
        boolean becomingActif = busUpdates.getStatut() == StatutTrajet.ACTIF;

        // Modifier les champs non-null
        if (busUpdates.getMarque() != null) existing.setMarque(busUpdates.getMarque());
        if (busUpdates.getModele() != null) existing.setModele(busUpdates.getModele());
        if (busUpdates.getImmatriculation() != null) existing.setImmatriculation(busUpdates.getImmatriculation());
        if (busUpdates.getCapacite() != null) existing.setCapacite(busUpdates.getCapacite());
        if (busUpdates.getTypeCarburant() != null) existing.setTypeCarburant(busUpdates.getTypeCarburant());
        if (busUpdates.getLigne() != null) existing.setLigne(busUpdates.getLigne());
        if (busUpdates.getHeureDepart() != null) existing.setHeureDepart(busUpdates.getHeureDepart());
        if (busUpdates.getDureeMinutes() != null) existing.setDureeMinutes(busUpdates.getDureeMinutes());
        if (busUpdates.getJoursDisponibles() != null) existing.setJoursDisponibles(busUpdates.getJoursDisponibles());
        if (busUpdates.getPlacesRestantes() != null) existing.setPlacesRestantes(busUpdates.getPlacesRestantes());
        if (busUpdates.getStatut() != null) existing.setStatut(busUpdates.getStatut());
        if (busUpdates.getPhotoUrl() != null) existing.setPhotoUrl(busUpdates.getPhotoUrl());

        Bus saved = busRepository.save(existing);

        // NOTIFICATION quand un bus inactif devient actif
        // NOTIFICATION quand un bus inactif devient actif
        if (wasInactif && becomingActif && existing.getPackId() != null) {
            List<Bus> busDuPack = busRepository.findByPackId(existing.getPackId());
            List<ReservationNavette> enAttente = new ArrayList<>();

            for (Bus bus : busDuPack) {
                List<ReservationNavette> reservations = reservationNavetteRepository
                        .findByBusIdAndStatut(bus.getId(), StatutReservation.EN_ATTENTE_ACTIVATION);
                enAttente.addAll(reservations);
            }

            for (ReservationNavette res : enAttente) {
                res.setStatut(StatutReservation.CONFIRME);
                reservationNavetteRepository.save(res);

                notificationService.envoyerNotification(
                        res.getEmployeId(),
                        "SYSTEM",
                        existing.getPackId(),
                        TypeNotification.ACTIVATION_BUS,
                        "Un bus de reserve a ete active ! Votre reservation est maintenant confirmee."
                );
            }

            if (!enAttente.isEmpty()) {
                notificationService.envoyerNotification(
                        "ADMIN",
                        "SYSTEM",
                        existing.getPackId(),
                        TypeNotification.ACTIVATION_BUS,
                        "Bus active avec succes. " + enAttente.size() + " employe(s) ont ete notifies."
                );
            }
        }

        return saved;
    }
    @Override
    public void delete(String id) {
        busRepository.deleteById(id);
    }
}