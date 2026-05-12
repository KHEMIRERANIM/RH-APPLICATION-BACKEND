package tn.esprit.rh_rse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RseActionRequest {

    private String employeeId;
    private String type;
    private String description;
}