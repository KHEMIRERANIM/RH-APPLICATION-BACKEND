package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String ancienPassword;
    private String nouveauPassword;
}