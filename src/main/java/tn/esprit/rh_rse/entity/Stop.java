package tn.esprit.rh_rse.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stop {
    private String name;
    private Double latitude;
    private Double longitude;
}
