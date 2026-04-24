// service/Formation/FraudService.java (version simplifiée sans WebSocket)
package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.Formation.FraudEventDTO;
import tn.esprit.rh_rse.dto.Formation.FraudResponseDTO;
import tn.esprit.rh_rse.dto.Formation.FraudSummaryDTO;
import tn.esprit.rh_rse.entity.Formation.FraudEvent;
import tn.esprit.rh_rse.repository.Formation.FraudEventRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudService {

    private final FraudEventRepository fraudEventRepository;
    private static final int MAX_VIOLATIONS = 1;

    public FraudResponseDTO registerFraudEvent(FraudEventDTO dto) {
        log.info("🚨 Événement de fraude reçu: {} - Employé: {}", dto.getEventType(), dto.getEmployeId());

        // Vérifier si déjà bloqué
        List<FraudEvent> existingBlocked = fraudEventRepository.findBlockedEvents(dto.getExamenId(), dto.getEmployeId());
        if (!existingBlocked.isEmpty()) {
            return FraudResponseDTO.builder()
                    .saved(false)
                    .totalViolations(MAX_VIOLATIONS)
                    .blocked(true)
                    .message("❌ Vous êtes déjà bloqué")
                    .remainingAttempts(0)
                    .build();
        }

        // Créer l'événement
        FraudEvent event = new FraudEvent(
                dto.getExamenId(),
                dto.getEmployeId(),
                dto.getEmployeNom() != null ? dto.getEmployeNom() : "Inconnu",
                dto.getEmployePrenom() != null ? dto.getEmployePrenom() : "Inconnu",
                dto.getEmployeEmail() != null ? dto.getEmployeEmail() : "",
                dto.getEventType(),
                dto.getTimestamp(),
                dto.getDescription()
        );

        fraudEventRepository.save(event);

        // Compter les violations
        long violationCount = fraudEventRepository.countViolations(dto.getExamenId(), dto.getEmployeId());
        boolean isBlocked = violationCount >= MAX_VIOLATIONS;

        if (isBlocked) {
            log.warn("🔒 Examen BLOQUÉ pour employé {}", dto.getEmployeId());
        }

        return FraudResponseDTO.builder()
                .saved(true)
                .totalViolations((int)violationCount)
                .blocked(isBlocked)
                .message(isBlocked ? "Examen bloqué" : "Violation enregistrée")
                .remainingAttempts((int) Math.max(0, MAX_VIOLATIONS - violationCount))
                .build();
    }

    public boolean isStudentBlocked(String examenId, String employeId) {
        long violationCount = fraudEventRepository.countViolations(examenId, employeId);
        return violationCount >= MAX_VIOLATIONS;
    }

    public long getViolationCount(String examenId, String employeId) {
        return fraudEventRepository.countViolations(examenId, employeId);
    }

    public Map<String, Object> getBlockedStatus(String examenId, String employeId) {
        long count = getViolationCount(examenId, employeId);
        boolean blocked = count >= MAX_VIOLATIONS;

        Map<String, Object> status = new HashMap<>();
        status.put("blocked", blocked);
        status.put("violationCount", count);
        status.put("maxViolations", MAX_VIOLATIONS);
        status.put("remainingAttempts", Math.max(0, MAX_VIOLATIONS - count));
        return status;
    }

    public List<FraudSummaryDTO> getExamFraudSummary(String examenId) {
        List<FraudEvent> events = fraudEventRepository.findByExamenIdOrderByTimestampDesc(examenId);

        Map<String, List<FraudEvent>> eventsByStudent = events.stream()
                .collect(Collectors.groupingBy(FraudEvent::getEmployeId));

        return eventsByStudent.entrySet().stream()
                .map(entry -> FraudSummaryDTO.builder()
                        .employeId(entry.getKey())
                        .employeNom(entry.getValue().get(0).getEmployeNom())
                        .employePrenom(entry.getValue().get(0).getEmployePrenom())
                        .violationCount((long) entry.getValue().size())
                        .isBlocked(entry.getValue().size() >= MAX_VIOLATIONS)
                        .build())
                .collect(Collectors.toList());
    }
}