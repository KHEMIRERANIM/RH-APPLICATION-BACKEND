package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;
import tn.esprit.rh_rse.exception.OffreNotFoundException;
import tn.esprit.rh_rse.exception.PartenaireNotFoundException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.PartenaireRepository;
import tn.esprit.rh_rse.service.OffreService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OffreServiceImpl implements OffreService {

    private final OffreRepository offreRepository;
    private final PartenaireRepository partenaireRepository;

    @Override
    public Offre creerOffre(Offre offre) {
        // Vérifier que le partenaire existe
        partenaireRepository.findById(offre.getIdPartenaire())
                .orElseThrow(() -> new PartenaireNotFoundException(offre.getIdPartenaire()));

        offre.setNbPlacesDispo(offre.getNbPlacesTotal());
        offre.setStatut("ACTIVE");
        offre.setCreatedAt(LocalDateTime.now());

        return offreRepository.save(offre);
    }

    @Override
    public Offre modifierOffre(String id, Offre offreModifiee) {
        Offre existante = offreRepository.findById(id)
                .orElseThrow(() -> new OffreNotFoundException(id));

        // Vérifier que le partenaire existe
        partenaireRepository.findById(offreModifiee.getIdPartenaire())
                .orElseThrow(() -> new PartenaireNotFoundException(offreModifiee.getIdPartenaire()));

        existante.setIdPartenaire(offreModifiee.getIdPartenaire());
        existante.setTitre(offreModifiee.getTitre());
        existante.setDescription(offreModifiee.getDescription());
        existante.setCategorie(offreModifiee.getCategorie());
        existante.setPrixReel(offreModifiee.getPrixReel());
        existante.setPrixConvention(offreModifiee.getPrixConvention());
        existante.setNbPlacesTotal(offreModifiee.getNbPlacesTotal());
        existante.setImageUrl(offreModifiee.getImageUrl());
        existante.setLocalisation(offreModifiee.getLocalisation());
        existante.setDateDebut(offreModifiee.getDateDebut());
        existante.setDateFin(offreModifiee.getDateFin());
        existante.setDetailsHotel(offreModifiee.getDetailsHotel());

        return offreRepository.save(existante);
    }

    @Override
    public void supprimerOffre(String id) {
        if (!offreRepository.existsById(id)) {
            throw new OffreNotFoundException(id);
        }
        offreRepository.deleteById(id);
    }

    @Override
    public Offre getById(String id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new OffreNotFoundException(id));
    }

    @Override
    public List<Offre> getAllActives() {
        List<Offre> offres = offreRepository.findByStatutAndDateFinGreaterThanEqual(
                "ACTIVE", LocalDate.now());

        return offres.stream()
                .filter(o -> estPartenaireActif(o.getIdPartenaire()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Offre> getByCategorie(CategorieOffre categorie) {
        List<Offre> offres = offreRepository.findByCategorieAndStatutAndDateFinGreaterThanEqual(
                categorie, "ACTIVE", LocalDate.now());

        return offres.stream()
                .filter(o -> estPartenaireActif(o.getIdPartenaire()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Offre> getByPartenaire(String idPartenaire) {
        return offreRepository.findByIdPartenaire(idPartenaire);
    }

    @Override
    public Offre toggleStatut(String id) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new OffreNotFoundException(id));

        if ("ACTIVE".equals(offre.getStatut())) {
            offre.setStatut("INACTIVE");
        } else {
            offre.setStatut("ACTIVE");
        }

        return offreRepository.save(offre);
    }

    private boolean estPartenaireActif(String idPartenaire) {
        return partenaireRepository.findById(idPartenaire)
                .map(Partenaire::isActif)
                .orElse(false);
    }
}