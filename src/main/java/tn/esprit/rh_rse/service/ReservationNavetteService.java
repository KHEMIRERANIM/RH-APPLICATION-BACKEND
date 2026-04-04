package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.ReservationNavetteRequest;
import tn.esprit.rh_rse.dto.response.ReservationNavetteResponse;
import java.util.List;

public interface ReservationNavetteService {
    List<ReservationNavetteResponse> getAll();
    ReservationNavetteResponse getById(String id);
    ReservationNavetteResponse create(ReservationNavetteRequest request);
    ReservationNavetteResponse update(String id, ReservationNavetteRequest request);
    void delete(String id);
    List<ReservationNavetteResponse> getByEmployeId(String employeId);
    int getTotalPointsByEmploye(String employeId);
}