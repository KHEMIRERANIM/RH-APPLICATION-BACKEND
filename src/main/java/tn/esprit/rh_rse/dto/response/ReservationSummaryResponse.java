package tn.esprit.rh_rse.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryResponse {
    private int nbConfirmes;
    private int nbEnAttente;
    private int nbIgnored; // Already reserved
    private List<String> datesIgnored;
}
