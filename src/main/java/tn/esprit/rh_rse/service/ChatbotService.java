package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.rh_rse.entity.BulletinSalaire;
import tn.esprit.rh_rse.entity.DemandeConge;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.repository.BulletinSalaireRepository;
import tn.esprit.rh_rse.repository.DemandeCongeRepository;
import tn.esprit.rh_rse.repository.SoldeCongeRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final UserRepository userRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final SoldeCongeRepository soldeCongeRepository;
    private final BulletinSalaireRepository bulletinSalaireRepository;

    // Pour le contexte (se souvenir de la dernière demande)
    private final Map<String, String> dernierContexte = new HashMap<>();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(10 * 1024 * 1024))
            .build();

    // =========================================================================
    // MÉTHODE PRINCIPALE
    // =========================================================================

    public Mono<String> repondre(String employeId, String message) {
        return Mono.fromCallable(() -> {
            try {
                User employe = userRepository.findById(employeId).orElse(null);
                if (employe == null) {
                    return "Désolé, je ne vous reconnais pas. Veuillez vous reconnecter.";
                }

                String messageLower = message.toLowerCase();

                // 1. D'abord, essayer les questions RH (base de données)
                String reponseRH = traiterQuestionRH(employe, messageLower);
                if (reponseRH != null) {
                    return reponseRH;
                }

                // 2. Sinon, utiliser l'IA locale via Ollama
                return appelerOllama(employe, message);

            } catch (Exception e) {
                e.printStackTrace();
                return "Désolé, je rencontre une difficulté technique. Veuillez réessayer.";
            }
        });
    }

    // =========================================================================
    // MODULE RH - Questions spécifiques (base de données)
    // =========================================================================

    private String traiterQuestionRH(User employe, String message) {

        // ============================================================
        // 1. DEMANDES REFUSÉES AVEC NOMBRE DE JOURS
        // ============================================================
        if (message.contains("refusé") || message.contains("refuse")) {
            return getDemandesRefusees(employe);
        }

        // ============================================================
        // 2. DEMANDES APPROUVÉES AVEC DÉTAILS
        // ============================================================
        if ((message.contains("approuvé") || message.contains("approuve")) &&
                (message.contains("détail") || message.contains("detail") ||
                        message.contains("liste") || message.contains("tous"))) {
            return getCongesApprouvesDetail(employe);
        }

        // ============================================================
        // 3. RÉCAPITULATIF COMPLET (approuvés + refusés + en attente)
        // ============================================================
        if (message.contains("récapitulatif") || message.contains("bilan") ||
                (message.contains("tous") && message.contains("congé"))) {
            return getRecapitulatifConges(employe);
        }

        // ============================================================
        // 4. NOMBRE DE JOURS POUR UNE DEMANDE
        // ============================================================
        if ((message.contains("combien") && message.contains("jour")) ||
                message.contains("nombre de jours") ||
                message.contains("de combien")) {
            return getNombreJoursDemande(employe);
        }

        // ============================================================
        // 5. DEMANDE EN ATTENTE AVEC DÉTAILS
        // ============================================================
        if (message.contains("attente") && (message.contains("détail") || message.contains("detail") ||
                message.contains("info") || message.contains("information"))) {
            return getDetailDemandeEnAttente(employe);
        }

        // ============================================================
        // 6. CONGÉS APPROUVÉS (simple)
        // ============================================================
        if (message.contains("approuvé") || message.contains("accepté")) {
            return getCongesApprouves(employe);
        }

        // ============================================================
        // 7. DEMANDES EN ATTENTE (simple)
        // ============================================================
        if (message.contains("attente") || message.contains("en cours")) {
            return getDemandesEnAttente(employe);
        }

        // ============================================================
        // 8. SOLDE DE CONGÉS
        // ============================================================
        if (message.contains("solde") || message.contains("reste")) {
            return getSoldeConges(employe);
        }

        // ============================================================
        // 9. PROCÉDURE
        // ============================================================
        if (message.contains("comment") || message.contains("procédure")) {
            return getProcedureConge();
        }

        // ============================================================
        // 10. RÈGLES
        // ============================================================
        if (message.contains("règle") || message.contains("préavis")) {
            return getReglesConges();
        }

        // ============================================================
        // 11. SALAIRES
        // ============================================================
        if (message.contains("salaire") || message.contains("bulletin")) {
            return getDernierSalaire(employe);
        }
        if (message.contains("prime")) {
            return getPrimes(employe);
        }

        // ============================================================
        // 12. INFOS EMPLOYÉ
        // ============================================================
        if (message.contains("mon poste") || message.contains("mon role")) {
            return "Votre poste : **" + (employe.getPoste() != null ? employe.getPoste() : "Non défini") + "**\n" +
                    "Département : **" + (employe.getDepartement() != null ? employe.getDepartement() : "Non défini") + "**";
        }
        if (message.contains("mon manager") || message.contains("mon responsable")) {
            if (employe.getManagerId() != null) {
                User manager = userRepository.findById(employe.getManagerId()).orElse(null);
                if (manager != null) {
                    return "Votre manager : **" + manager.getPrenom() + " " + manager.getNom() + "**";
                }
            }
            return "Vous n'avez pas de manager assigné.";
        }

        // ============================================================
        // 13. STATISTIQUES
        // ============================================================
        if (message.contains("statistique")) {
            return getStatsConges(employe);
        }

        // ============================================================
        // 14. AIDE
        // ============================================================
        if (message.contains("aide") || message.contains("help")) {
            return getAide();
        }

        // ============================================================
        // 15. BONJOUR
        // ============================================================
        if (message.contains("bonjour") || message.contains("salut")) {
            return "Bonjour " + employe.getPrenom() + " ! \n\n" +
                    "Je suis votre assistant RH intelligent (alimenté par Mistral via Ollama).\n\n" +
                    "Tapez **'aide'** pour voir toutes les commandes disponibles.\n\n" +
                    "Nouvelles commandes :\n" +
                    "• 'mes demandes refusées' → Voir les refus avec détails\n" +
                    "• 'récapitulatif' → Bilan complet de vos congés\n" +
                    "• 'détail demande' → Infos sur votre demande en attente";
        }

        return null;
    }

    // =========================================================================
    // MÉTHODES RH (Données réelles)
    // =========================================================================

    private String getSoldeConges(User employe) {
        int annee = LocalDate.now().getYear();
        var soldeOpt = soldeCongeRepository.findByEmployeIdAndAnnee(employe.getId(), annee);

        if (soldeOpt.isPresent()) {
            var solde = soldeOpt.get();
            return String.format(
                    "Votre solde de congés (%d) :\n\n" +
                            "┌─────────────────────────┐\n" +
                            "│ Jours total      : %3d  │\n" +
                            "│ Jours utilisés   : %3d  │\n" +
                            "│ En attente       : %3d  │\n" +
                            "├─────────────────────────┤\n" +
                            "│ ✅ **RESTANTS**   : %3d  │\n" +
                            "└─────────────────────────┘",
                    annee, solde.getJoursTotal(), solde.getJoursUtilises(),
                    solde.getJoursEnAttente(), solde.getJoursRestants()
            );
        }
        return "Vous avez 30 jours de congés disponibles cette année.";
    }

    private String getDemandesEnAttente(User employe) {
        List<DemandeConge> demandes = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.EN_ATTENTE);

        if (demandes.isEmpty()) {
            return "Aucune demande en attente.";
        }

        int totalJours = demandes.stream().mapToInt(DemandeConge::getNombreJours).sum();

        return String.format("Vous avez %d demande(s) en attente (total: %d jours) de validation.",
                demandes.size(), totalJours);
    }

    private String getCongesApprouves(User employe) {
        List<DemandeConge> demandes = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.APPROUVE);

        if (demandes.isEmpty()) {
            return "Aucun congé approuvé pour le moment.";
        }

        int totalJours = demandes.stream().mapToInt(DemandeConge::getNombreJours).sum();

        return String.format("Vous avez %d congé(s) approuvé(s) (total: %d jours).",
                demandes.size(), totalJours);
    }

    // NOUVELLE MÉTHODE : Demandes refusées avec détails
    private String getDemandesRefusees(User employe) {
        List<DemandeConge> demandesRefusees = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.REFUSE);

        if (demandesRefusees.isEmpty()) {
            return "Aucune demande refusée pour le moment.\n\n" +
                    "Toutes vos demandes ont été acceptées ou sont en attente.";
        }

        int totalJoursRefuses = demandesRefusees.stream()
                .mapToInt(DemandeConge::getNombreJours)
                .sum();

        String details = demandesRefusees.stream()
                .map(d -> String.format(
                        "   Du %s au %s → %d jours\n       Motif du refus : %s",
                        d.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        d.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        d.getNombreJours(),
                        d.getCommentaireManager() != null ? d.getCommentaireManager() : "Non spécifié"
                ))
                .collect(Collectors.joining("\n\n"));

        if (demandesRefusees.size() == 1) {
            return String.format(
                    "Vous avez 1 demande refusée :\n\n%s\n\n" +
                            "Total de jours perdus : %d jours\n\n" +
                            "Conseil : Contactez votre manager pour comprendre le motif du refus.",
                    details, totalJoursRefuses
            );
        } else {
            return String.format(
                    "Vous avez %d demandes refusées :\n\n%s\n\n" +
                            "Total de jours perdus : %d jours\n\n" +
                            "Conseil : Discutez avec votre manager avant de refaire une demande.",
                    demandesRefusees.size(), details, totalJoursRefuses
            );
        }
    }

    // NOUVELLE MÉTHODE : Congés approuvés avec détails
    private String getCongesApprouvesDetail(User employe) {
        List<DemandeConge> demandesApprouvees = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.APPROUVE);

        if (demandesApprouvees.isEmpty()) {
            return "Aucun congé approuvé pour le moment.";
        }

        int totalJoursApprouves = demandesApprouvees.stream()
                .mapToInt(DemandeConge::getNombreJours)
                .sum();

        String details = demandesApprouvees.stream()
                .map(d -> String.format(
                        "    Du %s au %s → %d jours (Type: %s)",
                        d.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        d.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        d.getNombreJours(),
                        getTypeLabel(d.getType().toString())
                ))
                .collect(Collectors.joining("\n\n"));

        return String.format(
                "Vous avez %d congé(s) approuvé(s) :\n\n%s\n\n" +
                        "Total de jours pris : %d jours",
                demandesApprouvees.size(), details, totalJoursApprouves
        );
    }

    // NOUVELLE MÉTHODE : Récapitulatif complet
    private String getRecapitulatifConges(User employe) {
        List<DemandeConge> approuvees = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.APPROUVE);
        List<DemandeConge> refusees = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.REFUSE);
        List<DemandeConge> enAttente = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.EN_ATTENTE);

        int joursApprouves = approuvees.stream().mapToInt(DemandeConge::getNombreJours).sum();
        int joursRefuses = refusees.stream().mapToInt(DemandeConge::getNombreJours).sum();
        int joursEnAttente = enAttente.stream().mapToInt(DemandeConge::getNombreJours).sum();

        return String.format(
                "RÉCAPITULATIF DE VOS CONGÉS\n\n" +
                        "┌─────────────────────────────────────┐\n" +
                        "│ Approuvés : %2d demande(s) → %2d jours │\n" +
                        "│ Refusés   : %2d demande(s) → %2d jours │\n" +
                        "│ En attente : %2d demande(s) → %2d jours │\n" +
                        "├─────────────────────────────────────┤\n" +
                        "│ Total     : %2d demande(s) → %2d jours │\n" +
                        "└─────────────────────────────────────┘",
                approuvees.size(), joursApprouves,
                refusees.size(), joursRefuses,
                enAttente.size(), joursEnAttente,
                approuvees.size() + refusees.size() + enAttente.size(),
                joursApprouves + joursRefuses + joursEnAttente
        );
    }

    // NOUVELLE MÉTHODE : Nombre de jours d'une demande
    private String getNombreJoursDemande(User employe) {
        // D'abord vérifier les demandes en attente
        List<DemandeConge> enAttente = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.EN_ATTENTE);

        if (!enAttente.isEmpty()) {
            DemandeConge d = enAttente.get(0);
            return String.format(
                    "Votre demande en attente :\n" +
                            "• Du %s au %s\n" +
                            "• **%d jours** au total\n" +
                            "• Motif : %s",
                    d.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    d.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    d.getNombreJours(),
                    d.getMotif() != null && !d.getMotif().isEmpty() ? d.getMotif() : "Non spécifié"
            );
        }

        // Sinon, vérifier les dernières demandes approuvées
        List<DemandeConge> approuvees = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.APPROUVE);

        if (!approuvees.isEmpty()) {
            DemandeConge d = approuvees.get(0);
            return String.format(
                    "Votre dernier congé approuvé :\n" +
                            "• Du %s au %s\n" +
                            "• **%d jours**",
                    d.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    d.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    d.getNombreJours()
            );
        }

        return "Vous n'avez aucune demande de congé pour laquelle je peux vous donner le nombre de jours.";
    }

    // NOUVELLE MÉTHODE : Détail de la demande en attente
    private String getDetailDemandeEnAttente(User employe) {
        List<DemandeConge> enAttente = demandeCongeRepository
                .findByEmployeIdAndStatut(employe.getId(), StatutConge.EN_ATTENTE);

        if (enAttente.isEmpty()) {
            return "Aucune demande en attente.";
        }

        DemandeConge d = enAttente.get(0);
        return String.format(
                "Détail de votre demande en attente :\n\n" +
                        "Période : %s → %s\n" +
                        "Durée : **%d jours**\n" +
                        "Motif : %s\n" +
                        "Type : %s\n\n" +
                        "En attente de validation par votre manager.",
                d.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                d.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                d.getNombreJours(),
                d.getMotif() != null && !d.getMotif().isEmpty() ? d.getMotif() : "Non spécifié",
                getTypeLabel(d.getType().toString())
        );
    }

    private String getDernierSalaire(User employe) {
        List<BulletinSalaire> bulletins = bulletinSalaireRepository.findByEmployeId(employe.getId());
        if (bulletins.isEmpty()) {
            return "Aucun bulletin de salaire disponible.";
        }
        BulletinSalaire dernier = bulletins.get(bulletins.size() - 1);
        String moisLabel = getMoisLabel(dernier.getMois());
        return "Dernier salaire (" + moisLabel + " " + dernier.getAnnee() + ") : " +
                dernier.getSalaireNet() + " TND";
    }

    private String getPrimes(User employe) {
        List<BulletinSalaire> bulletins = bulletinSalaireRepository.findByEmployeId(employe.getId());
        if (bulletins.isEmpty()) {
            return "💰 Aucune information sur les primes.";
        }
        double total = bulletins.stream()
                .mapToDouble(b -> b.getPrimes() + b.getHeuresSupplementaires())
                .sum();
        return "💰 **Total des primes perçues** : " + total + " TND";
    }

    private String getStatsConges(User employe) {
        List<DemandeConge> demandes = demandeCongeRepository.findByEmployeId(employe.getId());
        long approuves = demandes.stream().filter(d -> d.getStatut() == StatutConge.APPROUVE).count();
        long refuses = demandes.stream().filter(d -> d.getStatut() == StatutConge.REFUSE).count();
        long enAttente = demandes.stream().filter(d -> d.getStatut() == StatutConge.EN_ATTENTE).count();

        return String.format(
                "Statistiques de vos congés :\n\n" +
                        "• Approuvées : %d\n" +
                        "• Refusées : %d\n" +
                        "• En attente : %d\n" +
                        "• Taux d'acceptation : %.1f%%",
                approuves, refuses, enAttente,
                (approuves + refuses) > 0 ? (approuves * 100.0 / (approuves + refuses)) : 0
        );
    }

    private String getProcedureConge() {
        return "Pour faire une demande :\n" +
                "1. Allez dans 'Mes Congés'\n" +
                "2. Cliquez sur 'Nouvelle demande'\n" +
                "3. Remplissez le formulaire\n" +
                "4. Envoyez → réponse sous 48h";
    }

    private String getReglesConges() {
        return " **Règles** :\n• Préavis : 48h\n• Périodes chargées : juillet-août\n• Congé maladie : certificat requis";
    }

    private String getAide() {
        return "Assistant RH Intelligent (Mistral via Ollama)\n\n" +
                " Questions RH :\n" +
                "• 'solde' → Voir mon solde\n" +
                "• 'attente' → Demandes en attente\n" +
                "• 'approuvé' → Congés approuvés\n" +
                "• 'détail demande' → Détails de la demande en attente\n" +
                "• 'mes demandes refusées' → Demandes refusées avec détails\n" +
                "• 'récapitulatif' → Bilan complet de tous vos congés\n" +
                "• 'salaire' → Dernier bulletin\n" +
                "• 'primes' → Mes primes\n" +
                "• 'procédure' → Comment demander\n" +
                "• 'règles' → Règles des congés\n" +
                "• 'statistiques' → Mes stats\n" +
                "• 'mon poste' → Mon poste\n" +
                "• 'mon manager' → Mon responsable\n\n" +
                " Questions générales (IA) :\n" +
                "• Posez n'importe quelle question !\n" +
                "  Ex: 'Raconte-moi une blague', 'Quel temps fait-il ?', 'Qui est le président ?'\n\n" +
                " Besoin d'aide ? Contactez le RH : rh@entreprise.com";
    }

    private String getMoisLabel(int mois) {
        String[] moisLabels = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return moisLabels[mois - 1];
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case "CONGE_ANNUEL": return "Annuel";
            case "CONGE_MALADIE": return "Maladie";
            case "CONGE_MATERNITE": return "Maternité";
            case "CONGE_PATERNITE": return "Paternité";
            case "CONGE_SANS_SOLDE": return "Sans solde";
            default: return "Autre";
        }
    }

    // =========================================================================
    // MODULE IA - Ollama (local, gratuit)
    // =========================================================================

    private String appelerOllama(User employe, String message) {
        try {
            System.out.println(" Appel à Ollama (Mistral) pour: " + message);

            String prompt = construirePromptIA(employe, message);

            Map<String, Object> response = webClient.post()
                    .uri("http://localhost:11434/api/generate")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", "mistral",
                            "prompt", prompt,
                            "stream", false,
                            "options", Map.of(
                                    "temperature", 0.7,
                                    "top_p", 0.9
                            )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(45))
                    .block();

            if (response != null && response.containsKey("response")) {
                String reponse = response.get("response").toString();
                System.out.println("Réponse Ollama reçue (" + reponse.length() + " caractères)");
                return  reponse.trim();
            }

            return " Je n'ai pas pu générer de réponse. Veuillez reformuler.";

        } catch (Exception e) {
            System.err.println("Erreur Ollama: " + e.getMessage());
            return "**Ollama n'est pas démarré.**\n\n" +
                    "Pour activer l'IA locale (gratuite) :\n" +
                    "1. Téléchargez Ollama sur https://ollama.com\n" +
                    "2. Ouvrez un terminal et exécutez : ollama pull mistral\n" +
                    "3. Puis : ollama serve\n" +
                    "4. Redémarrez Spring Boot\n\n" +
                    "En attendant, je réponds aux questions RH. Tapez **'aide'** pour voir les commandes.";
        }
    }

    private String construirePromptIA(User employe, String question) {
        return String.format("""
            Tu es un assistant RH professionnel, sympathique et expert en ressources humaines.
            Tu travailles dans une entreprise tunisienne.
            
            Informations sur l'employé qui te parle :
            - Prénom : %s
            - Poste : %s
            - Département : %s
            
            Règles :
            - Réponds TOUJOURS en français
            - Sois concis (max 3-4 phrases si possible)
            - Sois poli et utile
            - Si tu ne sais pas, dis-le honnêtement
            
            L'employé demande : %s
            
            Réponds de manière appropriée :
            """,
                employe.getPrenom(),
                employe.getPoste() != null ? employe.getPoste() : "Employé",
                employe.getDepartement() != null ? employe.getDepartement() : "Général",
                question
        );
    }
}