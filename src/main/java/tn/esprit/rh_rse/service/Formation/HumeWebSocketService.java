package tn.esprit.rh_rse.service.Formation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class HumeWebSocketService {

    @Value("${hume.api.key}")
    private String humeApiKey;

    private WebSocketSession session;
    private CompletableFuture<String> lastResult = new CompletableFuture<>();

    public void connect() {
        String wsUrl = "wss://api.hume.ai/v0/evi/chat?api_key=" + humeApiKey;

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                client,
                new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        log.info("✅ Connecté à Hume EVI");
                        HumeWebSocketService.this.session = session;
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        log.info("📨 Message reçu: {}", message.getPayload());
                        lastResult.complete(message.getPayload());
                        lastResult = new CompletableFuture<>();
                    }
                },
                wsUrl
        );

        manager.start();
    }

    public void sendAudio(byte[] audioData) {
        if (session != null && session.isOpen()) {
            try {
                String base64Audio = Base64.getEncoder().encodeToString(audioData);
                session.sendMessage(new TextMessage(
                        "{\"type\":\"audio_input\",\"data\":\"" + base64Audio + "\"}"
                ));
            } catch (Exception e) {
                log.error("Erreur envoi audio", e);
            }
        }
    }

    public CompletableFuture<String> analyzeRealtime(byte[] audioData) {
        lastResult = new CompletableFuture<>();
        sendAudio(audioData);
        return lastResult;
    }
}