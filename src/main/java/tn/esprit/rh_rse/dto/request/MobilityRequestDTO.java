package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class MobilityRequestDTO {

    private String targetCareerId;
    private String motivationLetter;
    private String motivationFileName;
    private String motivationFileBase64;
}