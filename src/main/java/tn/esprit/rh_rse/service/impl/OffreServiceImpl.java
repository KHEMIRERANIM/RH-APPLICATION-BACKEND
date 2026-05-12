package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.CreateOffreRequest;
import tn.esprit.rh_rse.dto.request.UpdateOffreRequest;
import tn.esprit.rh_rse.dto.response.OffreResponse;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.StatutOffre;
import tn.esprit.rh_rse.entity.enums.TypeContrat;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.service.OffreService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OffreServiceImpl implements OffreService {

    private final OffreRepository offreRepository;

    @Override
    public OffreResponse createOffre(CreateOffreRequest request, String createurId) {
        Offre offre = Offre.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .departement(request.getDepartement())
                .localisation(request.getLocalisation())
                .typeContrat(request.getTypeContrat())
                .niveauExperience(request.getNiveauExperience())
                .niveauEtudes(request.getNiveauEtudes())
                .salaireMin(request.getSalaireMin())
                .salaireMax(request.getSalaireMax())
                .competencesRequises(request.getCompetencesRequises())
                .avantages(request.getAvantages())
                .nombrePostes(request.getNombrePostes())
                .dateExpiration(request.getDateExpiration())
                .statut(StatutOffre.BROUILLON)
                .createurId(createurId)
                .nombreCandidatures(0)
                .biasDetected(false)
                .dateCreation(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(offreRepository.save(offre));
    }

    @Override
    public List<OffreResponse> getAllOffresPubliees() {
        return offreRepository.findByStatut(StatutOffre.PUBLIEE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<OffreResponse> getOffresFiltered(String departement, TypeContrat typeContrat,
                                                 String localisation, String search) {
        if (search != null && !search.isEmpty())
            return offreRepository.findByTitreContainingIgnoreCaseAndStatut(search, StatutOffre.PUBLIEE)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        if (departement != null && !departement.isEmpty())
            return offreRepository.findByStatutAndDepartement(StatutOffre.PUBLIEE, departement)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        if (typeContrat != null)
            return offreRepository.findByStatutAndTypeContrat(StatutOffre.PUBLIEE, typeContrat)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        if (localisation != null && !localisation.isEmpty())
            return offreRepository.findByStatutAndLocalisation(StatutOffre.PUBLIEE, localisation)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        return getAllOffresPubliees();
    }

    @Override
    public OffreResponse getOffreById(String id) {
        return toResponse(offreRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + id)));
    }

    @Override
    public OffreResponse updateOffre(String id, UpdateOffreRequest request) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + id));
        if (request.getTitre() != null)               offre.setTitre(request.getTitre());
        if (request.getDescription() != null)         offre.setDescription(request.getDescription());
        if (request.getDepartement() != null)         offre.setDepartement(request.getDepartement());
        if (request.getLocalisation() != null)        offre.setLocalisation(request.getLocalisation());
        if (request.getTypeContrat() != null)         offre.setTypeContrat(request.getTypeContrat());
        if (request.getNiveauExperience() != null)    offre.setNiveauExperience(request.getNiveauExperience());
        if (request.getNiveauEtudes() != null)        offre.setNiveauEtudes(request.getNiveauEtudes());
        if (request.getSalaireMin() != null)          offre.setSalaireMin(request.getSalaireMin());
        if (request.getSalaireMax() != null)          offre.setSalaireMax(request.getSalaireMax());
        if (request.getCompetencesRequises() != null) offre.setCompetencesRequises(request.getCompetencesRequises());
        if (request.getAvantages() != null)           offre.setAvantages(request.getAvantages());
        if (request.getNombrePostes() > 0)            offre.setNombrePostes(request.getNombrePostes());
        if (request.getDateExpiration() != null)      offre.setDateExpiration(request.getDateExpiration());
        offre.setUpdatedAt(LocalDateTime.now());
        return toResponse(offreRepository.save(offre));
    }

    @Override
    public void publierOffre(String id) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + id));
        offre.setStatut(StatutOffre.PUBLIEE);
        offre.setDatePublication(LocalDateTime.now());
        offre.setUpdatedAt(LocalDateTime.now());
        offreRepository.save(offre);
    }

    @Override
    public void cloturerOffre(String id) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + id));
        offre.setStatut(StatutOffre.CLOTUREE);
        offre.setUpdatedAt(LocalDateTime.now());
        offreRepository.save(offre);
    }

    @Override
    public void archiverOffre(String id) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + id));
        offre.setStatut(StatutOffre.ARCHIVEE);
        offre.setUpdatedAt(LocalDateTime.now());
        offreRepository.save(offre);
    }

    @Override
    public void deleteOffre(String id) {
        if (!offreRepository.existsById(id))
            throw new RecrutementNotFoundException("Offre introuvable : " + id);
        offreRepository.deleteById(id);
    }

    @Override
    public List<OffreResponse> getOffresByCreateurId(String createurId) {
        return offreRepository.findByCreateurId(createurId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private OffreResponse toResponse(Offre o) {
        return OffreResponse.builder()
                .id(o.getId())
                .titre(o.getTitre())
                .description(o.getDescription())
                .departement(o.getDepartement())
                .localisation(o.getLocalisation())
                .typeContrat(o.getTypeContrat())
                .niveauExperience(o.getNiveauExperience())
                .niveauEtudes(o.getNiveauEtudes())
                .salaireMin(o.getSalaireMin())
                .salaireMax(o.getSalaireMax())
                .competencesRequises(o.getCompetencesRequises())
                .avantages(o.getAvantages())
                .statut(o.getStatut())
                .biasDetected(o.isBiasDetected())
                .createurId(o.getCreateurId())
                .nombrePostes(o.getNombrePostes())
                .nombreCandidatures(o.getNombreCandidatures())
                .dateCreation(o.getDateCreation())
                .datePublication(o.getDatePublication())
                .dateExpiration(o.getDateExpiration())
                .build();
    }
}