package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;

@Data
public class MobilityReviewDTO {
    private MobilityStatus status;
    private String reviewedBy;
    private String reviewComment;
}