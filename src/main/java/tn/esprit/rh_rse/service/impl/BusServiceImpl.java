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
        if (wasInactif && becomingActif && existing.getPackId() != null) {
            List<Bus> busDuPack = busRepository.findByPackId(existing.getPackId());
            
            // 1. Récupérer toutes les réservations EN_ATTENTE_ACTIVATION du pack, triées par date
            List<ReservationNavette> allWaitlisted = new ArrayList<>();
            for (Bus b : busDuPack) {
                allWaitlisted.addAll(reservationNavetteRepository.findByBusIdAndStatut(
                        b.getId(), StatutReservation.EN_ATTENTE_ACTIVATION));
            }
            allWaitlisted.sort((a, b) -> a.getDateReservation().compareTo(b.getDateReservation()));

            // 2. Transférer intelligemment jusqu'à saturation de la capacité
            int capacite = existing.getCapacite();
            int transferredCount = 0;

            for (ReservationNavette res : allWaitlisted) {
                if (transferredCount >= capacite) break;

                // Transfert effectif
                res.setBusId(existing.getId());
                
                // Migration de tous les jours en attente vers confirmés
                if (res.getJoursEnAttente() != null && !res.getJoursEnAttente().isEmpty()) {
                    if (res.getJoursConfirmes() == null) res.setJoursConfirmes(new ArrayList<>());
                    res.getJoursConfirmes().addAll(res.getJoursEnAttente());
                    res.setJoursEnAttente(new ArrayList<>());
                    res.setStatut(StatutReservation.CONFIRME);
                }
                
                reservationNavetteRepository.save(res);
                transferredCount++;

                notificationService.envoyerNotification(
                        res.getEmployeId(),
                        "SYSTEM",
                        existing.getPackId(),
                        TypeNotification.ACTIVATION_BUS,
                        "Bonne nouvelle ! Le bus " + existing.getMarque() + " est actif. Votre réservation est CONFIRMÉE."
                );
            }

            if (transferredCount > 0) {
                notificationService.envoyerNotification(
                        "ADMIN", "SYSTEM", existing.getPackId(), TypeNotification.ACTIVATION_BUS,
                        "Activation globale : " + transferredCount + " passagers transférés sur le nouveau bus actif."
                );
            }
        }

        return saved;
    }
    @Override
    public Bus activateForDay(String busId, String date) {
        Bus bus = getById(busId);
        if (bus.getPackId() == null) {
            throw new RuntimeException("Ce bus n'appartient pas à un pack.");
        }

        // On n'active plus le bus globalement ici, on laisse son statut tel quel (ex: INACTIF)
        Bus savedBus = bus;

        // 2. Récupérer toutes les réservations en attente pour le PACK
        List<Bus> packBuses = busRepository.findByPackId(bus.getPackId());
        
        // On récupère toutes les réservations EN_ATTENTE_ACTIVATION du pack
        // et on les trie par date de création (premier arrivé, premier servi)
        List<ReservationNavette> allWaitlisted = new ArrayList<>();
        for (Bus b : packBuses) {
            allWaitlisted.addAll(reservationNavetteRepository.findByBusIdAndStatut(
                    b.getId(), StatutReservation.EN_ATTENTE_ACTIVATION));
        }
        
        allWaitlisted.sort((a, b) -> a.getDateReservation().compareTo(b.getDateReservation()));

        // 3. Filtrer pour le jour dédié (Comparaison de texte brute YYYY-MM-DD)
        String targetDateStr = date.length() >= 10 ? date.substring(0, 10) : date;
        List<ReservationNavette> waitlistedForDay = allWaitlisted.stream()
                .filter(r -> r.getJoursEnAttente() != null && 
                             r.getJoursEnAttente().stream().anyMatch(d -> (d.length() >= 10 ? d.substring(0, 10) : d).equals(targetDateStr)))
                .toList();

        // 4. Calculer la capacité disponible
        int capacite = bus.getCapacite();
        int transferredCount = 0;

        for (ReservationNavette res : waitlistedForDay) {
            if (transferredCount >= capacite) break;

            // Transfert du jour spécifique (Isolation par texte brute)
            res.getJoursEnAttente().removeIf(d -> (d.length() >= 10 ? d.substring(0, 10) : d).equals(targetDateStr));
            
            if (res.getJoursConfirmes() == null) res.setJoursConfirmes(new ArrayList<>());
            if (!res.getJoursConfirmes().contains(targetDateStr)) {
                res.getJoursConfirmes().add(targetDateStr);
            }
            
            // Mise à jour de l'ID du bus
            res.setBusId(bus.getId());

            // Si plus aucun jour n'est en attente, le statut global devient CONFIRME
            if (res.getJoursEnAttente().isEmpty()) {
                res.setStatut(StatutReservation.CONFIRME);
            }

            reservationNavetteRepository.save(res);
            transferredCount++;

            // Notification
            notificationService.envoyerNotification(
                    res.getEmployeId(),
                    "SYSTEM",
                    bus.getPackId(),
                    TypeNotification.ACTIVATION_BUS,
                    "Votre réservation pour le " + date + " est CONFIRMÉE sur le nouveau bus " + bus.getMarque() + "."
            );
        }

        // Notification Admin
        notificationService.envoyerNotification(
                "ADMIN", "SYSTEM", bus.getPackId(), TypeNotification.ACTIVATION_BUS,
                "Bus activé pour le " + date + ". " + transferredCount + " passagers transférés."
        );

        return savedBus;
    }

    @Override
    public void delete(String id) {
        busRepository.deleteById(id);
    }
}