package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.DetailsHotel;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.exception.OffreNotFoundException;
import tn.esprit.rh_rse.exception.PlacesIndisponiblesException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.AvantageReservationRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.EmailService;
import tn.esprit.rh_rse.service.PdfGenerationService;
import tn.esprit.rh_rse.service.AvantageReservationService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvantageReservationServiceImpl implements AvantageReservationService {

    private final AvantageReservationRepository reservationRepository;
    private final OffreRepository       offreRepository;
    private final UserRepository        userRepository;
    private final PdfGenerationService  pdfGenerationService;
    private final EmailService          emailService;

    // ══════════════════════════════════════════════════════════════════
    // RÉSERVATION STANDARD (VOYAGE / FESTIVAL)
    // ══════════════════════════════════════════════════════════════════

    @Override
    public AvantageReservation reserverOuModifier(String idUser, String idOffre, Integer nbPersonnes) {
        Offre offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new OffreNotFoundException(idOffre));

        Optional<AvantageReservation> existante = reservationRepository
                .findByIdUserAndIdOffreAndStatut(idUser, idOffre, StatutReservation.CONFIRMEE);

        if (existante.isPresent()) {
            return modifierReservationStandard(existante.get(), offre, nbPersonnes);
        }
        return creerReservationStandard(idUser, offre, nbPersonnes);
    }

    private AvantageReservation creerReservationStandard(String idUser, Offre offre, int nbPersonnes) {
        if (offre.getNbPlacesDispo() < nbPersonnes) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - nbPersonnes);
        offreRepository.save(offre);

        AvantageReservation reservation = AvantageReservation.builder()
                .idUser(idUser)
                .idOffre(offre.getId())
                .nbPersonnes(nbPersonnes)
                .prixUnitaire(offre.getPrixConvention())
                .prixTotal(offre.getPrixConvention() * nbPersonnes)
                .statut(StatutReservation.CONFIRMEE)
                .dateReservation(LocalDateTime.now())
                .build();

        AvantageReservation saved = reservationRepository.save(reservation);
        _envoyerEmail(saved, offre, idUser);
        return saved;
    }

    private AvantageReservation modifierReservationStandard(AvantageReservation reservation, Offre offre, int nouveauNb) {
        int diff = nouveauNb - reservation.getNbPersonnes();
        if (diff > 0 && offre.getNbPlacesDispo() < diff) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }
        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - diff);
        offreRepository.save(offre);

        reservation.setNbPersonnes(nouveauNb);
        reservation.setPrixTotal(offre.getPrixConvention() * nouveauNb);
        return reservationRepository.save(reservation);
    }

    // ══════════════════════════════════════════════════════════════════
    // RÉSERVATION HÔTELIÈRE (adultes + enfants + formule pension)
    // ══════════════════════════════════════════════════════════════════

    @Override
    public AvantageReservation reserverHotel(String idUser, String idOffre,
                                     Integer nbAdultes, Integer nbEnfants, String formule, LocalDate checkIn, LocalDate checkOut) {
        Offre offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new OffreNotFoundException(idOffre));

        if (offre.getDetailsHotel() == null) {
            throw new RuntimeException("Cette offre n'est pas de type hôtelier");
        }
        
        if (checkIn == null || checkOut == null || checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new RuntimeException("Dates de réservation invalides");
        }

        int nbTotal = (nbAdultes != null ? nbAdultes : 0) + (nbEnfants != null ? nbEnfants : 0);
        if (nbTotal < 1) {
            throw new RuntimeException("Le nombre de personnes doit être au moins 1");
        }

        Optional<AvantageReservation> existante = reservationRepository
                .findByIdUserAndIdOffreAndStatut(idUser, idOffre, StatutReservation.CONFIRMEE);

        if (existante.isPresent()) {
            return modifierReservationHotel(existante.get(), offre, nbAdultes, nbEnfants, formule, checkIn, checkOut);
        }
        return creerReservationHotel(idUser, offre, nbAdultes, nbEnfants, formule, checkIn, checkOut);
    }

    private AvantageReservation creerReservationHotel(String idUser, Offre offre,
                                               int nbAdultes, int nbEnfants, String formule, LocalDate checkIn, LocalDate checkOut) {
        int nbTotal = nbAdultes + nbEnfants;
        if (offre.getNbPlacesDispo() < nbTotal) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }

        int nuits = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        int minNuits = offre.getDetailsHotel().getNombreNuits() != null ? offre.getDetailsHotel().getNombreNuits() : 1;
        if (nuits < minNuits) {
            throw new RuntimeException("La durée minimale du séjour pour cet hôtel est de " + minNuits + " nuit(s).");
        }
        double prix = _calculerPrixHotel(offre.getDetailsHotel(), nbAdultes, nbEnfants, formule, nuits);

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - nbTotal);
        offreRepository.save(offre);

        AvantageReservation reservation = AvantageReservation.builder()
                .idUser(idUser)
                .idOffre(offre.getId())
                .nbPersonnes(nbTotal)
                .nbAdultes(nbAdultes)
                .nbEnfants(nbEnfants)
                .formule(formule)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .prixUnitaire(offre.getDetailsHotel().getPrixAdulte())
                .prixTotal(prix)
                .statut(StatutReservation.CONFIRMEE)
                .dateReservation(LocalDateTime.now())
                .build();

        AvantageReservation saved = reservationRepository.save(reservation);
        _envoyerEmail(saved, offre, idUser);
        return saved;
    }

    @Override
    public AvantageReservation creerReservationHotel(String idUser, String idOffre, Integer nbPersonnesChoisi) {
        // Fallback or implementation of method mistakenly added to service interface, ignoring.
        return null;
    }

    private AvantageReservation modifierReservationHotel(AvantageReservation reservation, Offre offre,
                                                  int nbAdultes, int nbEnfants, String formule, LocalDate checkIn, LocalDate checkOut) {
        int ancienNb = reservation.getNbPersonnes();
        int nouveauNb = nbAdultes + nbEnfants;
        int diff = nouveauNb - ancienNb;

        if (diff > 0 && offre.getNbPlacesDispo() < diff) {
            throw new PlacesIndisponiblesException(offre.getNbPlacesDispo());
        }

        int nuits = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        int minNuits = offre.getDetailsHotel().getNombreNuits() != null ? offre.getDetailsHotel().getNombreNuits() : 1;
        if (nuits < minNuits) {
            throw new RuntimeException("La durée minimale du séjour pour cet hôtel est de " + minNuits + " nuit(s).");
        }
        double prix = _calculerPrixHotel(offre.getDetailsHotel(), nbAdultes, nbEnfants, formule, nuits);

        offre.setNbPlacesDispo(offre.getNbPlacesDispo() - diff);
        offreRepository.save(offre);

        reservation.setNbPersonnes(nouveauNb);
        reservation.setNbAdultes(nbAdultes);
        reservation.setNbEnfants(nbEnfants);
        reservation.setFormule(formule);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setPrixTotal(prix);

        return reservationRepository.save(reservation);
    }

    /**
     * Calcul du prix total hôtel dynamique
     */
    private double _calculerPrixHotel(DetailsHotel d, int nbAdultes, int nbEnfants, String formule, int nuits) {
        double pA   = d.getPrixAdulte()  != null ? d.getPrixAdulte()  : 0.0;
        double pE   = d.getPrixEnfant()  != null ? d.getPrixEnfant()  : 0.0;
        double supx = 0.0;

        if (formule != null && d.getSurprixFormules() != null
                && d.getSurprixFormules().containsKey(formule)) {
            supx = d.getSurprixFormules().get(formule);
        }

        double parNuit = (nbAdultes * pA) + (nbEnfants * pE) + (supx * (nbAdultes + nbEnfants));
        return Math.round(parNuit * nuits * 100.0) / 100.0;
    }

    // ══════════════════════════════════════════════════════════════════
    // ANNULATION
    // ══════════════════════════════════════════════════════════════════

    @Override
    public AvantageReservation annuler(String idUser, String idReservation) {
        AvantageReservation reservation = reservationRepository.findById(idReservation)
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

    // ══════════════════════════════════════════════════════════════════
    // LECTURES
    // ══════════════════════════════════════════════════════════════════

    @Override
    public List<AvantageReservation> getMesReservations(String idUser) {
        return reservationRepository.findByIdUser(idUser);
    }

    @Override
    public List<AvantageReservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public List<AvantageReservation> getByOffre(String idOffre) {
        return reservationRepository.findByIdOffre(idOffre);
    }

    @Override
    public void viderAnnulees(String idUser) {
        reservationRepository.deleteByIdUserAndStatut(idUser, StatutReservation.ANNULEE);
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPER PRIVÉ : envoi email/PDF
    // ══════════════════════════════════════════════════════════════════

    private void _envoyerEmail(AvantageReservation saved, Offre offre, String idUser) {
        try {
            User user = userRepository.findById(idUser).orElse(null);
            if (user != null && user.getEmail() != null) {
                byte[] pdfBytes = pdfGenerationService.generateReservationPdf(saved, offre, user);
                emailService.sendReservationConfirmation(
                        user.getEmail(),
                        user.getPrenom() + " " + user.getNom(),
                        offre.getTitre(),
                        pdfBytes
                );
            }
        } catch (Exception e) {
            System.err.println("Could not send email/pdf: " + e.getMessage());
        }
    }
}
