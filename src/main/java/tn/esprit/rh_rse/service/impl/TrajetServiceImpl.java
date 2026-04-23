package tn.esprit.rh_rse.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.repository.TrajetRepository;
import tn.esprit.rh_rse.service.TrajetService;

import java.time.LocalDate;
import java.util.List;

import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.ReservationRepository;
import tn.esprit.rh_rse.service.NotificationService;

@Service
@RequiredArgsConstructor
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    @Override
    public List<Trajet> getAll() {
        List<Trajet> trajets = trajetRepository.findAll();
        trajets.forEach(t -> {
            if (t.getPrix() == null) t.setPrix(5.0);
        });
        return trajets;
    }

    @Override
    public Trajet getById(String id) {
        Trajet trajet = trajetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé : " + id));
        if (trajet.getPrix() == null) {
            trajet.setPrix(5.0);
        }
        return trajet;
    }

    @Override
    public Trajet create(Trajet trajet) {
        trajet.setDateCreation(LocalDate.now());
        trajet.setPlacesRestantes(trajet.getPlacesDisponibles());
        trajet.setStatut(StatutTrajet.ACTIF);
        return trajetRepository.save(trajet);
    }

    @Override
    public Trajet update(String id, Trajet trajet) {
        Trajet existing = getById(id);
        existing.setAdresseDepart(trajet.getAdresseDepart());
        existing.setAdresseArrivee(trajet.getAdresseArrivee());
        existing.setHeureDepart(trajet.getHeureDepart());
        existing.setJoursDisponibles(trajet.getJoursDisponibles());
        existing.setPlacesDisponibles(trajet.getPlacesDisponibles());
        existing.setCategorie(trajet.getCategorie());
        existing.setStatut(trajet.getStatut());
        existing.setVehiculeId(trajet.getVehiculeId());
        existing.setPrix(trajet.getPrix());
        return trajetRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        trajetRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(String id, StatutTrajet statut) {
        Trajet t = trajetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trajet introuvable : " + id));
        t.setStatut(statut);
        trajetRepository.save(t);
    }

    @Override
    public List<Trajet> getByStatut(StatutTrajet statut) {
        List<Trajet> trajets = trajetRepository.findByStatut(statut);
        trajets.forEach(t -> {
            if (t.getPrix() == null) t.setPrix(5.0);
        });
        return trajets;
    }


    @Override
    @Transactional
    public void annulerTrajetConducteur(String trajetId) {
        Trajet trajet = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new RuntimeException("Trajet introuvable : " + trajetId));
        trajet.setStatut(StatutTrajet.INACTIF);
        trajetRepository.save(trajet);

        List<Reservation> reservations = reservationRepository.findByTrajetId(trajetId);
        for (Reservation res : reservations) {
            if (res.getStatut() != StatutReservation.ANNULE) {
                res.setStatut(StatutReservation.ANNULE);
                reservationRepository.save(res);
                notificationService.envoyerNotification(
                        res.getEmployeId(),
                        "SYSTEME",
                        trajetId,
                        TypeNotification.ALTERNATIVES_DISPONIBLES,
                        "Le conducteur a annule le trajet. Consultez les alternatives disponibles."
                );
            }
        }
    }
}