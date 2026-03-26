package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.exception.OffreNotFoundException;
import tn.esprit.rh_rse.exception.PlacesIndisponiblesException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.ReservationRepository;
import tn.esprit.rh_rse.service.ReservationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final OffreRepository offreRepository;

    @Override
    public Reservation reserverOuModifier(String idUser, String idOffre, Integer nbPersonnes) {
        Offre offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new OffreNotFoundException(idOffre));

        Optional<Reservation> reservationExistante = reservationRepository
                .findByIdUserAndIdOffreAndStatut(
                        idUser, idOffre, StatutReservation.CONFIRMEE);

        if (reservationExistante.isPresent()) {
            return modifierReservation(reservationExistante.get(), offre, nbPersonnes);
        }

        return creerReservation(idUser, offre, nbPersonnes);
    }

    private Reservation creerReservation(String idUser, Offre offre, int nbPersonnes) {
        if (offre.getNbPlacesDispo() < nbPersonnes) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - nbPersonnes);
        offreRepository.save(offre);

        Reservation reservation = Reservation.builder()
                .idUser(idUser)
                .idOffre(offre.getId())
                .nbPersonnes(nbPersonnes)
                .prixUnitaire(offre.getPrixConvention())
                .prixTotal(offre.getPrixConvention() * nbPersonnes)
                .statut(StatutReservation.CONFIRMEE)
                .dateReservation(LocalDateTime.now())
                .build();

        return reservationRepository.save(reservation);
    }

    private Reservation modifierReservation(Reservation reservation, Offre offre, int nouveauNbPersonnes) {
        int ancienNb = reservation.getNbPersonnes();
        int difference = nouveauNbPersonnes - ancienNb;

        if (difference > 0 && offre.getNbPlacesDispo() < difference) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - difference);
        offreRepository.save(offre);

        reservation.setNbPersonnes(nouveauNbPersonnes);
        reservation.setPrixTotal(offre.getPrixConvention() * nouveauNbPersonnes);

        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation annuler(String idUser, String idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (!reservation.getIdUser().equals(idUser)) {
            throw new RuntimeException("Accès refusé à cette réservation");
        }

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new RuntimeException("Cette réservation est déjà annulée");
        }

        Offre offre = offreRepository.findById(reservation.getIdOffre())
                .orElseThrow(() -> new OffreNotFoundException(reservation.getIdOffre()));

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() + reservation.getNbPersonnes());
        offreRepository.save(offre);

        reservation.setStatut(StatutReservation.ANNULEE);
        reservation.setDateAnnulation(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    @Override
    public List<Reservation> getMesReservations(String idUser) {
        return reservationRepository.findByIdUser(idUser);
    }

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public List<Reservation> getByOffre(String idOffre) {
        return reservationRepository.findByIdOffre(idOffre);
    }
}