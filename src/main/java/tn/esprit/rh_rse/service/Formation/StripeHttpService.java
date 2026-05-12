package tn.esprit.rh_rse.service.Formation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class StripeHttpService {

    private static final Logger log = LoggerFactory.getLogger(StripeHttpService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public String createPaymentIntent(String userId, int points, int amountEuros) {
        try {
            String url = "https://api.stripe.com/v1/payment_intents";

            // Authentification Basic avec la clé Stripe
            String auth = stripeSecretKey + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Corps de la requête
            String body = String.format(
                    "amount=%d&currency=eur&metadata[userId]=%s&metadata[points]=%d&automatic_payment_methods[enabled]=true",
                    amountEuros * 100L, userId, points
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String clientSecret = (String) response.getBody().get("client_secret");
                log.info("✅ PaymentIntent créé: {}", clientSecret);
                return clientSecret;
            } else {
                throw new RuntimeException("Erreur Stripe: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Erreur création PaymentIntent: {}", e.getMessage());
            throw new RuntimeException("Erreur Stripe: " + e.getMessage());
        }
    }
}