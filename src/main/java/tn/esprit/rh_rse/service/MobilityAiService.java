package tn.esprit.rh_rse.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.dto.request.MobilityPredictionRequest;
import tn.esprit.rh_rse.dto.response.MobilityPredictionResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class MobilityAiService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String PYTHON_API_URL =
            "http://127.0.0.1:8001/predict-mobility";

    public MobilityPredictionResponse predict(MobilityPredictionRequest request) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();

        body.put("current_job", request.getCurrentJob().toLowerCase());
        body.put("experience_years", request.getExperienceYears());
        body.put("python_years", request.getPythonYears());
        body.put("sql_years", request.getSqlYears());
        body.put("java_years", request.getJavaYears());
        body.put("angular_years", request.getAngularYears());
        body.put("react_years", request.getReactYears());
        body.put("cloud_years", request.getCloudYears());
        body.put("devops_years", request.getDevopsYears());
        body.put("security_years", request.getSecurityYears());
        body.put("ml_years", request.getMlYears());
        body.put("testing_years", request.getTestingYears());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<MobilityPredictionResponse> response =
                restTemplate.exchange(
                        PYTHON_API_URL,
                        HttpMethod.POST,
                        entity,
                        MobilityPredictionResponse.class
                );

        return response.getBody();
    }
}
