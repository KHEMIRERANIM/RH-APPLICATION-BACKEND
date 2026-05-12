package tn.esprit.rh_rse.dto.response.conge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommandationEmployeResponse {
    private String type;
    private String icon;
    private String titre;
    private String description;
    private LocalDate dateSuggestion;
}
