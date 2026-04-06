package tn.esprit.rh_rse.service;

import java.util.List;
import tn.esprit.rh_rse.dto.response.StatAvantageDTOs.*;

public interface StatAvantageService {
    KpiDto getKpis();
    List<StatCategorieDto> getStatParCategorie();
    List<StatTopOffreDto> getTop5Offres();
    List<StatMensuelleDto> getStatParMois(Integer annee);
    List<StatStatutDto> getStatStatuts();
}
