package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.Bus;

import java.util.List;

@Data
public class BusPackRequest {
    private Bus busData;
    /** Nombre de bus actifs (≥ 1). */
    private int activeCount;
    /** Nombre de bus inactifs en réserve (≥ 0). */
    private int inactiveCount;
    private List<Integer> busCapacities; // ← ajouter

}
