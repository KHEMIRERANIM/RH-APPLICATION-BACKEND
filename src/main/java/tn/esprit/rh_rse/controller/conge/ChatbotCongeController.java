package tn.esprit.rh_rse.controller.conge;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tn.esprit.rh_rse.dto.request.conge.ChatbotRequest;
import tn.esprit.rh_rse.service.conge.ChatbotCongeService;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotCongeController {

    private final ChatbotCongeService chatbotCongeService;

    @PostMapping("/message")
    public Mono<ResponseEntity<Map<String, String>>> envoyerMessage(
            @RequestBody ChatbotRequest request) {

        return chatbotCongeService.repondre(request.getEmployeId(), request.getMessage())
                .map(reponse -> ResponseEntity.ok(Map.of("reponse", reponse)))
                .onErrorReturn(ResponseEntity.internalServerError()
                        .body(Map.of("reponse", "Désolé, le service est temporairement indisponible.")));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chatbot API fonctionne !");
    }
}
