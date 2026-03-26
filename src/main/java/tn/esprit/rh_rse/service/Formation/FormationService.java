package tn.esprit.rh_rse.service.Formation;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.entity.Formation.EvaluationFormation;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.repository.Formation.EvaluationFormationRepository;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.Formation.InscriptionFormationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationService {

    private final FormationRepository formationRepository;
    private final InscriptionFormationRepository inscriptionRepository;
    private final EvaluationFormationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public List<FormationDTO> getAllFormations() {
        return formationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormationDTO> getFormationsDisponibles() {
        return formationRepository.findFormationsDisponibles(LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormationDTO getFormationById(String id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        return convertToDTO(formation);
    }

    @Transactional
    public FormationDTO createFormation(FormationDTO formationDTO) {
        Formation formation = new Formation();
        formation.setTitre(formationDTO.getTitre());
        formation.setDescription(formationDTO.getDescription());
        formation.setObjectifs(formationDTO.getObjectifs());
        formation.setPreRequis(formationDTO.getPreRequis());
        formation.setType(formationDTO.getType());
        formation.setDureeHeures(formationDTO.getDureeHeures());
        formation.setNombrePlaces(formationDTO.getNombrePlaces());
        formation.setPlacesDisponibles(formationDTO.getNombrePlaces());
        formation.setNiveau(formationDTO.getNiveau());
        formation.setFormateur(formationDTO.getFormateur());
        formation.setFormateurBio(formationDTO.getFormateurBio());
        formation.setLieu(formationDTO.getLieu());
        formation.setLienVisio(formationDTO.getLienVisio());
        formation.setDateDebut(formationDTO.getDateDebut());
        formation.setDateFin(formationDTO.getDateFin());
        formation.setDateLimiteInscription(formationDTO.getDateLimiteInscription());
        formation.setImageUrl(formationDTO.getImageUrl());
        formation.setActive(true);
        formation.setCreatedAt(LocalDateTime.now());
        formation.setUpdatedAt(LocalDateTime.now());

        Formation savedFormation = formationRepository.save(formation);
        return convertToDTO(savedFormation);
    }

    @Transactional
    public FormationDTO updateFormation(String id, FormationDTO formationDTO) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        formation.setTitre(formationDTO.getTitre());
        formation.setDescription(formationDTO.getDescription());
        formation.setObjectifs(formationDTO.getObjectifs());
        formation.setPreRequis(formationDTO.getPreRequis());
        formation.setDureeHeures(formationDTO.getDureeHeures());
        formation.setNombrePlaces(formationDTO.getNombrePlaces());
        formation.setLieu(formationDTO.getLieu());
        formation.setLienVisio(formationDTO.getLienVisio());
        formation.setDateDebut(formationDTO.getDateDebut());
        formation.setDateFin(formationDTO.getDateFin());
        formation.setDateLimiteInscription(formationDTO.getDateLimiteInscription());
        formation.setUpdatedAt(LocalDateTime.now());

        Formation updatedFormation = formationRepository.save(formation);
        return convertToDTO(updatedFormation);
    }

    @Transactional
    public void deleteFormation(String id) {
        formationRepository.deleteById(id);
    }

    @Transactional
    public void toggleActive(String id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        formation.setActive(!formation.getActive());
        formation.setUpdatedAt(LocalDateTime.now());
        formationRepository.save(formation);
    }

    private FormationDTO convertToDTO(Formation formation) {
        FormationDTO dto = new FormationDTO();
        dto.setId(formation.getId());
        dto.setTitre(formation.getTitre());
        dto.setDescription(formation.getDescription());
        dto.setObjectifs(formation.getObjectifs());
        dto.setPreRequis(formation.getPreRequis());
        dto.setType(formation.getType());
        dto.setDureeHeures(formation.getDureeHeures());
        dto.setNombrePlaces(formation.getNombrePlaces());
        dto.setPlacesDisponibles(formation.getPlacesDisponibles());
        dto.setNiveau(formation.getNiveau());
        dto.setFormateur(formation.getFormateur());
        dto.setFormateurBio(formation.getFormateurBio());
        dto.setLieu(formation.getLieu());
        dto.setLienVisio(formation.getLienVisio());
        dto.setDateDebut(formation.getDateDebut());
        dto.setDateFin(formation.getDateFin());
        dto.setDateLimiteInscription(formation.getDateLimiteInscription());
        dto.setImageUrl(formation.getImageUrl());
        dto.setActive(formation.getActive());

        // Calculer la note moyenne
        List<EvaluationFormation> evaluations = evaluationRepository.findByFormationId(formation.getId());
        double moyenne = evaluations.stream()
                .mapToInt(EvaluationFormation::getNote)
                .average()
                .orElse(0.0);
        dto.setNoteMoyenne(Math.round(moyenne * 10) / 10.0);

        // Compter les inscriptions confirmées
        int nbInscrits = inscriptionRepository.countInscriptionsConfirmees(formation.getId());
        dto.setNombreInscrits(nbInscrits);

        return dto;
    }
}