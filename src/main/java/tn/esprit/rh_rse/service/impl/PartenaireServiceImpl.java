package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.exception.PartenaireNotFoundException;
import tn.esprit.rh_rse.repository.PartenaireRepository;
import tn.esprit.rh_rse.service.PartenaireService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartenaireServiceImpl implements PartenaireService {

    private final PartenaireRepository partenaireRepository;

    @Override
    public Partenaire creerPartenaire(Partenaire partenaire) {
        partenaire.setActif(true);
        partenaire.setCreatedAt(LocalDateTime.now());
        return partenaireRepository.save(partenaire);
    }

    @Override
    public Partenaire modifierPartenaire(String id, Partenaire partenaireModifie) {
        Partenaire existant = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException(id));

        existant.setNom(partenaireModifie.getNom());
        existant.setType(partenaireModifie.getType());
        existant.setLogoUrl(partenaireModifie.getLogoUrl());
        existant.setEmailContact(partenaireModifie.getEmailContact());
        existant.setDateConvention(partenaireModifie.getDateConvention());

        return partenaireRepository.save(existant);
    }

    @Override
    public void supprimerPartenaire(String id) {
        if (!partenaireRepository.existsById(id)) {
            throw new PartenaireNotFoundException(id);
        }
        partenaireRepository.deleteById(id);
    }

    @Override
    public Partenaire getById(String id) {
        return partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException(id));
    }

    @Override
    public List<Partenaire> getAll() {
        return partenaireRepository.findAll();
    }

    @Override
    public Partenaire toggleActif(String id) {
        Partenaire partenaire = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException(id));
        partenaire.setActif(!partenaire.isActif());
        return partenaireRepository.save(partenaire);
    }
}