package tn.esprit.rh_rse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionDTO {
    private String vehiculeId;
    private Double latitude;
    private Double longitude;
}