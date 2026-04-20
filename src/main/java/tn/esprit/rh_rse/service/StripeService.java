package tn.esprit.rh_rse.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Crée un PaymentIntent Stripe en utilisant un Token généré par le frontend.
     * Cette méthode est conforme aux règles de sécurité de Stripe (PCI).
     */
    public PaymentIntent createPaymentIntent(String reservationId, Double montant, String stripeToken) throws StripeException {
        // Stripe attend des montants en centimes
        long unitAmount = (long) (montant * 100);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", unitAmount);
        params.put("currency", "eur");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("reservationId", reservationId);
        params.put("metadata", metadata);

        if (stripeToken != null && !stripeToken.isEmpty()) {
            // 1. Créer une PaymentMethod à partir du Token
            PaymentMethodCreateParams pmParams = PaymentMethodCreateParams.builder()
                    .setType(PaymentMethodCreateParams.Type.CARD)
                    .setCard(PaymentMethodCreateParams.Token.builder().setToken(stripeToken).build())
                    .build();
            
            PaymentMethod pm = PaymentMethod.create(pmParams);

            // 2. Attacher la PaymentMethod au PaymentIntent
            params.put("payment_method", pm.getId());
            params.put("confirm", true);
            params.put("return_url", "http://localhost:4200/apps/covoiturage/user/dashboard");
        }

        return PaymentIntent.create(params);
    }

    /**
     * Rembourse un paiement Stripe lié à une réservation.
     * Recherche le PaymentIntent grâce à la metadata 'reservationId'.
     */
    public void refundPayment(String reservationId) throws StripeException {
        // Stripe ne permet pas de rembourser directement par metadata, 
        // on liste donc les derniers paiements pour trouver le bon.
        Map<String, Object> listParams = new HashMap<>();
        listParams.put("limit", 100); 
        
        Iterable<PaymentIntent> paymentIntents = PaymentIntent.list(listParams).autoPagingIterable();
        
        for (PaymentIntent intent : paymentIntents) {
            String resIdMetadata = intent.getMetadata().get("reservationId");
            if (reservationId.equals(resIdMetadata) && "succeeded".equals(intent.getStatus())) {
                Map<String, Object> refundParams = new HashMap<>();
                refundParams.put("payment_intent", intent.getId());
                com.stripe.model.Refund.create(refundParams);
                return;
            }
        }
        
        throw new RuntimeException("Aucun paiement réussi trouvé pour la réservation : " + reservationId);
    }
}