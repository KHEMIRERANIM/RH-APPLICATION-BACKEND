package tn.esprit.rh_rse.dto.request;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReservationNavetteRequest {
    private String busId;
    private String employeId;
    private List<LocalDate> dates;
}
