// ChatbotController.java - Ajouter sessionId
package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.ChatbotRequestDTO;
import tn.esprit.rh_rse.dto.Formation.ChatbotResponseDTO;
import tn.esprit.rh_rse.service.Formation.ChatbotService;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponseDTO> askQuestion(@RequestBody ChatbotRequestDTO request) {
        log.info(" Question reçue: {}", request.getQuestion());

        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        ChatbotResponseDTO response = chatbotService.askQuestion(request.getQuestion(), sessionId);

        log.info(" Réponse générée (succès: {})", response.isSuccess());
        return ResponseEntity.ok(response);
    }
}