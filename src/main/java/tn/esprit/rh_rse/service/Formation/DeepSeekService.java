package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.rh_rse.dto.Formation.KPIsDTO;
import tn.esprit.rh_rse.dto.Formation.RapportIADTO;
import tn.esprit.rh_rse.entity.Formation.Formation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeepSeekService {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DeepSeekService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Générer le rapport IA complet pour le gérant
     */
    public RapportIADTO genererRapportComplet(KPIsDTO kpis, List<Formation> formations) {
        RapportIADTO rapport = new RapportIADTO();
        rapport.setDateGeneration(LocalDateTime.now());
        rapport.setPeriode(getPeriode());
        rapport.setTitre("Rapport Stratégique des Formations");
        rapport.setVersion("1.0");

        // Vérifier si la clé API est configurée
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk-votre-cle-api-deepseek")) {
            log.warn("Clé API DeepSeek non configurée. Utilisation du rapport local.");
            genererRapportLocal(kpis, rapport);
            return rapport;
        }

        try {
            // Préparer les données pour DeepSeek
            String donneesFormatees = formaterDonneesPourRapport(kpis, formations);

            // Appeler DeepSeek
            String analyseIA = appelerDeepSeek(donneesFormatees);

            // Parser la réponse
            parserReponseIA(analyseIA, rapport, kpis);

            log.info("✅ Rapport DeepSeek généré avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur DeepSeek: {}", e.getMessage());
            rapport.setStatut("ERREUR");
            genererRapportLocal(kpis, rapport);
        }

        return rapport;
    }

    /**
     * Appel à l'API DeepSeek
     */
    private String appelerDeepSeek(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Tu es un expert en stratégie RH et formation professionnelle. " +
                                            "Tu rédiges des rapports pour les dirigeants d'entreprise. " +
                                            "Ton style est professionnel, clair, et orienté action."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.6,
                    "max_tokens", 2000
            );

            String response = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extraireContenu(response);

        } catch (Exception e) {
            log.error("Erreur appel DeepSeek: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Formater les données pour le prompt
     */
    private String formaterDonneesPourRapport(KPIsDTO kpis, List<Formation> formations) {
        StringBuilder sb = new StringBuilder();

        sb.append("Voici les données de la plateforme de formation :\n\n");
        sb.append("INDICATEURS GLOBAUX:\n");
        sb.append(String.format("- Taux de remplissage moyen: %.1f%%\n", kpis.getTauxRemplissageMoyen()));
        sb.append(String.format("- Taux de réussite global: %.1f%%\n", kpis.getTauxReussiteGlobal()));
        sb.append(String.format("- Total inscriptions: %d\n", kpis.getTotalInscrits()));
        sb.append(String.format("- Total certifiés: %d\n", kpis.getTotalCertifies()));
        sb.append(String.format("- Taux d'abandon: %.1f%%\n",
                kpis.getTotalInscrits() > 0 ? (kpis.getTotalAbandons() * 100.0) / kpis.getTotalInscrits() : 0));
        sb.append(String.format("- NPS moyen: %.1f\n", kpis.getNpsMoyen()));
        sb.append(String.format("- CA estimé: %.2f DT\n", kpis.getChiffreAffairesTotal()));
        sb.append(String.format("- Risque d'échec prédit par IA: %.1f%%\n\n", kpis.getTauxRisqueEchecGlobal()));

        sb.append("DEMANDE: Rédige un rapport stratégique avec résumé exécutif, analyse, recommandations et prévisions.\n");

        return sb.toString();
    }

    /**
     * Parser la réponse IA et remplir le DTO
     */
    private void parserReponseIA(String reponseIA, RapportIADTO rapport, KPIsDTO kpis) {
        if (reponseIA == null || reponseIA.isEmpty()) {
            rapport.setStatut("ERREUR");
            genererRapportLocal(kpis, rapport);
            return;
        }

        // Version simplifiée du parsing
        String contenu = reponseIA;

        // Essayer de séparer par sections
        if (contenu.contains("RÉSUMÉ") || contenu.contains("RESUME")) {
            int idxResume = contenu.indexOf("RÉSUMÉ");
            if (idxResume == -1) idxResume = contenu.indexOf("RESUME");

            int idxAnalyse = contenu.indexOf("ANALYSE");
            if (idxAnalyse == -1) idxAnalyse = contenu.indexOf("ANALYSE DÉTAILLÉE");

            int idxRecommandations = contenu.indexOf("RECOMMANDATION");
            if (idxRecommandations == -1) idxRecommandations = contenu.indexOf("RECO");

            if (idxResume > 0 && idxAnalyse > 0) {
                rapport.setResumeExecutif(contenu.substring(idxResume, idxAnalyse).trim());
            }
            if (idxAnalyse > 0 && idxRecommandations > 0) {
                rapport.setAnalyseDetaillee(contenu.substring(idxAnalyse, idxRecommandations).trim());
            }
            if (idxRecommandations > 0) {
                rapport.setRecommandations(contenu.substring(idxRecommandations).trim());
            }
        } else {
            // Si pas de sections trouvées, mettre tout dans l'analyse
            rapport.setAnalyseDetaillee(contenu);
        }

        // Ajouter les KPIs clés
        String kpisSummary = String.format(
                "\n\n---\n**KPIs clés:** Taux remplissage: %.1f%% | Taux réussite: %.1f%% | Risque échec: %.1f%%",
                kpis.getTauxRemplissageMoyen(),
                kpis.getTauxReussiteGlobal(),
                kpis.getTauxRisqueEchecGlobal()
        );

        if (rapport.getAnalyseDetaillee() != null) {
            rapport.setAnalyseDetaillee(rapport.getAnalyseDetaillee() + kpisSummary);
        }

        rapport.setStatut("SUCCES");
    }

    /**
     * Générer un rapport local (fallback sans API)
     */
    private void genererRapportLocal(KPIsDTO kpis, RapportIADTO rapport) {
        StringBuilder sb = new StringBuilder();

        sb.append("## RÉSUMÉ EXÉCUTIF\n\n");
        if (kpis.getTauxRemplissageMoyen() > 70) {
            sb.append("✅ Excellente performance globale. ");
        } else if (kpis.getTauxRemplissageMoyen() > 50) {
            sb.append("👍 Performance satisfaisante. ");
        } else {
            sb.append("⚠️ Performance à améliorer. ");
        }
        sb.append(String.format("Taux de remplissage: %.1f%%, Taux de réussite: %.1f%%.\n\n",
                kpis.getTauxRemplissageMoyen(), kpis.getTauxReussiteGlobal()));

        sb.append("## ANALYSE DÉTAILLÉE\n\n");
        sb.append("**Forces:**\n");
        if (kpis.getTauxReussiteGlobal() > 70) {
            sb.append("- Bon taux de réussite global\n");
        }
        if (kpis.getNpsMoyen() > 60) {
            sb.append("- Bonne satisfaction des participants\n");
        }

        sb.append("\n**Points d'attention:**\n");
        if (kpis.getTauxRemplissageMoyen() < 50) {
            sb.append("- Taux de remplissage faible\n");
        }
        if (kpis.getTauxRisqueEchecGlobal() > 50) {
            sb.append("- Risque d'échec élevé\n");
        }

        sb.append("\n## RECOMMANDATIONS\n\n");
        if (kpis.getTauxRemplissageMoyen() < 60) {
            sb.append("1. Améliorer la communication sur les formations (Priorité Haute)\n");
        }
        if (kpis.getTauxRisqueEchecGlobal() > 50) {
            sb.append("2. Renforcer l'accompagnement pédagogique (Priorité Haute)\n");
        }
        sb.append("3. Mettre en place un système de feedback (Priorité Moyenne)\n");

        sb.append("\n## PRÉVISIONS\n\n");
        sb.append(String.format("Objectif trimestre prochain: atteindre %.1f%% de taux de remplissage.\n",
                kpis.getTauxRemplissageMoyen() + 10));

        rapport.setAnalyseDetaillee(sb.toString());
        rapport.setStatut("SUCCES_LOCAL");
    }

    /**
     * Extraire le contenu de la réponse JSON
     */
    private String extraireContenu(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Erreur parsing réponse", e);
            return null;
        }
    }

    /**
     * Obtenir la période actuelle
     */
    private String getPeriode() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }
}