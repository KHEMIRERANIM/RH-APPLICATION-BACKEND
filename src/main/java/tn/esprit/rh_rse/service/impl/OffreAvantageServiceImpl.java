package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;
import tn.esprit.rh_rse.exception.OffreAvantageNotFoundException;
import tn.esprit.rh_rse.exception.PartenaireNotFoundException;
import tn.esprit.rh_rse.repository.OffreAvantageRepository;
import tn.esprit.rh_rse.repository.PartenaireRepository;
import tn.esprit.rh_rse.service.OffreAvantageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OffreAvantageServiceImpl implements OffreAvantageService {

    private final OffreAvantageRepository offreAvantageRepository;
    private final PartenaireRepository partenaireRepository;

    @Override
    public OffreAvantage creerOffreAvantage(OffreAvantage offreAvantage) {
        // Vérifier que le partenaire existe
        partenaireRepository.findById(offreAvantage.getIdPartenaire())
                .orElseThrow(() -> new PartenaireNotFoundException(offreAvantage.getIdPartenaire()));

        offreAvantage.setNbPlacesDispo(offreAvantage.getNbPlacesTotal());
        offreAvantage.setStatut("ACTIVE");
        offreAvantage.setCreatedAt(LocalDateTime.now());

        return offreAvantageRepository.save(offreAvantage);
    }

    @Override
    public OffreAvantage modifierOffreAvantage(String id, OffreAvantage offreModifiee) {
        OffreAvantage existante = offreAvantageRepository.findById(id)
                .orElseThrow(() -> new OffreAvantageNotFoundException(id));

        // Vérifier que le partenaire existe
        partenaireRepository.findById(offreModifiee.getIdPartenaire())
                .orElseThrow(() -> new PartenaireNotFoundException(offreModifiee.getIdPartenaire()));

        existante.setIdPartenaire(offreModifiee.getIdPartenaire());
        existante.setTitre(offreModifiee.getTitre());
        existante.setDescription(offreModifiee.getDescription());
        existante.setCategorie(offreModifiee.getCategorie());
        existante.setPrixReel(offreModifiee.getPrixReel());
        existante.setPrixConvention(offreModifiee.getPrixConvention());
        Integer ancienTotal = existante.getNbPlacesTotal() != null ? existante.getNbPlacesTotal() : 0;
        Integer nouveauTotal = offreModifiee.getNbPlacesTotal() != null ? offreModifiee.getNbPlacesTotal() : 0;
        if (!ancienTotal.equals(nouveauTotal)) {
            int difference = nouveauTotal - ancienTotal;
            int ancienDispo = existante.getNbPlacesDispo() != null ? existante.getNbPlacesDispo() : ancienTotal;
            existante.setNbPlacesDispo(Math.max(0, ancienDispo + difference));
        }
        existante.setNbPlacesTotal(nouveauTotal);
        existante.setImageUrl(offreModifiee.getImageUrl());
        existante.setLocalisation(offreModifiee.getLocalisation());
        existante.setDateDebut(offreModifiee.getDateDebut());
        existante.setDateFin(offreModifiee.getDateFin());
        existante.setDetailsHotel(offreModifiee.getDetailsHotel());

        return offreAvantageRepository.save(existante);
    }

    @Override
    public void supprimerOffreAvantage(String id) {
        if (!offreAvantageRepository.existsById(id)) {
            throw new OffreAvantageNotFoundException(id);
        }
        offreAvantageRepository.deleteById(id);
    }

    @Override
    public OffreAvantage getById(String id) {
        return offreAvantageRepository.findById(id)
                .orElseThrow(() -> new OffreAvantageNotFoundException(id));
    }

    @Override
    public List<OffreAvantage> getAllActives() {
        List<OffreAvantage> offres = offreAvantageRepository.findByStatutAndDateFinGreaterThanEqual(
                "ACTIVE", LocalDate.now());

        return offres.stream()
                .filter(o -> estPartenaireActif(o.getIdPartenaire()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OffreAvantage> getByCategorie(CategorieOffreAvantage categorie) {
        List<OffreAvantage> offres = offreAvantageRepository.findByCategorieAndStatutAndDateFinGreaterThanEqual(
                categorie, "ACTIVE", LocalDate.now());

        return offres.stream()
                .filter(o -> estPartenaireActif(o.getIdPartenaire()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OffreAvantage> getByPartenaire(String idPartenaire) {
        return offreAvantageRepository.findByIdPartenaire(idPartenaire);
    }

    @Override
    public OffreAvantage toggleStatut(String id) {
        OffreAvantage offreAvantage = offreAvantageRepository.findById(id)
                .orElseThrow(() -> new OffreAvantageNotFoundException(id));

        if ("ACTIVE".equals(offreAvantage.getStatut())) {
            offreAvantage.setStatut("INACTIVE");
        } else {
            offreAvantage.setStatut("ACTIVE");
        }

        return offreAvantageRepository.save(offreAvantage);
    }

    private boolean estPartenaireActif(String idPartenaire) {
        return partenaireRepository.findById(idPartenaire)
                .map(Partenaire::isActif)
                .orElse(false);
    }
}