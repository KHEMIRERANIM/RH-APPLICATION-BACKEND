package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.repository.CommandeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AlerteCommandeController {

    private final CommandeRepository commandeRepository;

    @GetMapping("/commandes-pretes/{userId}")
    public List<Map<String, Object>> getAlertesUser(@PathVariable String userId) {
        return commandeRepository.findAll().stream()
                .filter(c -> "prete".equals(c.getStatut()))
                .filter(c -> userId.equals(c.getUserId()))
                .filter(c -> c.getDatePrete() != null)
                .map(c -> {
                    long minutes = Duration.between(c.getDatePrete(), LocalDateTime.now()).toMinutes();
                    Map<String, Object> alerte = new HashMap<>();
                    alerte.put("commandeId",       c.getId());
                    alerte.put("codeRetrait",       c.getCodeRetrait() != null ? c.getCodeRetrait() : "");
                    alerte.put("minutesRestantes",  (int) Math.max(0, 2 - minutes));
                    alerte.put("enRetard",          minutes >= 1);
                    return alerte;
                })
                .collect(Collectors.toList());
    }
}