// dto/Formation/FormationPropositionDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.rh_rse.entity.Formation.FormationProposition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormationPropositionDTO {

    private String id;
    private String technologie;
    private String titre;
    private String description;
    private String objectifs;
    private Integer dureeHeures;
    private String niveau;
    private String publicCible;
    private String preRequis;
    private String type;
    private String source;
    private Double scoreIA;
    private String statut;
    private String commentaireValidation;
    private Integer nombreInteresses;
    private List<InteretEmployeDTO> employesInteresses;
    private LocalDateTime dateProposition;
    private LocalDateTime dateValidation;
    private String validePar;
    private String formationCreeeId;

    @Data
    public static class InteretEmployeDTO {
        private String employeId;
        private String employeNom;
        private String employeEmail;
        private LocalDateTime dateInteret;
    }

    // Constructeur depuis l'entité
    public static FormationPropositionDTO fromEntity(FormationProposition entity) {
        FormationPropositionDTO dto = new FormationPropositionDTO();
        dto.setId(entity.getId());
        dto.setTechnologie(entity.getTechnologie());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setObjectifs(entity.getObjectifs());
        dto.setDureeHeures(entity.getDureeHeures());
        dto.setNiveau(entity.getNiveau());
        dto.setPublicCible(entity.getPublicCible());
        dto.setPreRequis(entity.getPreRequis());
        dto.setType(entity.getType());
        dto.setSource(entity.getSource());
        dto.setScoreIA(entity.getScoreIA());
        dto.setStatut(entity.getStatut());
        dto.setCommentaireValidation(entity.getCommentaireValidation());
        dto.setNombreInteresses(entity.getEmployesInteresses() != null ? entity.getEmployesInteresses().size() : 0);
        dto.setDateProposition(entity.getDateProposition());
        dto.setDateValidation(entity.getDateValidation());
        dto.setValidePar(entity.getValidePar());
        dto.setFormationCreeeId(entity.getFormationCreeeId());

        if (entity.getEmployesInteresses() != null) {
            dto.setEmployesInteresses(entity.getEmployesInteresses().stream()
                    .map(interet -> {
                        InteretEmployeDTO interetDto = new InteretEmployeDTO();
                        interetDto.setEmployeId(interet.getEmployeId());
                        interetDto.setEmployeNom(interet.getEmployeNom());
                        interetDto.setEmployeEmail(interet.getEmployeEmail());
                        interetDto.setDateInteret(interet.getDateInteret());
                        return interetDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}