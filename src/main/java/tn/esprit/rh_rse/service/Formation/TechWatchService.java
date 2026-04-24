package tn.esprit.rh_rse.service.Formation;
// service/Formation/TechWatchService.java

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.dto.Formation.RecommendationDTO;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TechWatchService {

    private final WebClient webClient = WebClient.create();

    /**
     * Récupère les tendances GitHub (top 10 repos trending)
     */
    public List<String> getGitHubTrends() {
        try {
            String url = "https://github-trending-api.com/repositories?language=&since=daily";
            // Note: Utiliser une API ou parser la page GitHub
            return Arrays.asList(
                    "LangChain", "LlamaIndex", "AutoGPT", "RAGFlow",
                    "Vite", "Bun", "TailwindCSS", "Next.js 14",
                    "Kubernetes", "Terraform", "Prometheus",
                    "Rust", "Zig", "Mojo"
            );
        } catch (Exception e) {
            log.error("Erreur récupération tendances GitHub", e);
            return List.of();
        }
    }

    /**
     * Récupère les derniers articles ArXiv sur l'IA/ML
     */
    public List<Map<String, String>> getArxivPapers() {
        try {
            String url = "http://export.arxiv.org/api/query?search_query=cat:cs.AI&sortBy=submittedDate&max_results=10";
            // Simulation pour l'exemple
            return List.of(
                    Map.of("title", "RAG with Graph Neural Networks", "summary", "Combining retrieval and GNNs"),
                    Map.of("title", "Efficient Fine-tuning of LLMs", "summary", "LoRA and QLoRA improvements")
            );
        } catch (Exception e) {
            log.error("Erreur récupération ArXiv", e);
            return List.of();
        }
    }

    /**
     * Analyse les formations existantes pour détecter les lacunes technologiques
     */
    public List<RecommendationDTO> detectTechnologieGaps(List<FormationDTO> formations) {
        Set<String> technologiesExistantes = formations.stream()
                .flatMap(f -> extractTechnologies(f.getTitre() + " " + f.getDescription()).stream())
                .collect(Collectors.toSet());

        List<String> technologiesTendances = getGitHubTrends();

        List<RecommendationDTO> recommandations = new ArrayList<>();

        for (String tech : technologiesTendances) {
            boolean existe = technologiesExistantes.stream()
                    .anyMatch(t -> t.toLowerCase().contains(tech.toLowerCase()) ||
                            tech.toLowerCase().contains(t.toLowerCase()));

            if (!existe) {
                RecommendationDTO rec = new RecommendationDTO();
                rec.setTechnologie(tech);
                rec.setScore(calculateRelevanceScore(tech, formations));
                rec.setSource("GitHub Trending");
                rec.setPriorite(rec.getScore() > 70 ? "HAUTE" : "MOYENNE");
                rec.setSuggestionsFormations(generateFormationSuggestions(tech));
                recommandations.add(rec);
            }
        }

        return recommandations.stream()
                .sorted((a,b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<String> extractTechnologies(String text) {
        List<String> techs = Arrays.asList(
                "Java", "Python", "JavaScript", "TypeScript", "React", "Angular", "Vue",
                "Spring Boot", "Django", "Flask", "FastAPI", "Node.js",
                "Docker", "Kubernetes", "AWS", "Azure", "GCP",
                "TensorFlow", "PyTorch", "LangChain", "RAG", "LLM", "GPT",
                "LlamaIndex", "AutoGPT", "Vite", "Bun", "TailwindCSS",
                "Terraform", "Prometheus", "Rust", "Zig", "Next.js"
        );
        return techs.stream()
                .filter(tech -> text.toLowerCase().contains(tech.toLowerCase()))
                .collect(Collectors.toList());
    }

    private double calculateRelevanceScore(String technologie, List<FormationDTO> formations) {
        // Score basé sur la popularité GitHub + pertinence métier
        Random random = new Random();
        return 50 + random.nextDouble() * 50; // Simulation
    }

    private List<String> generateFormationSuggestions(String technologie) {
        return Arrays.asList(
                "Introduction à " + technologie + " (Débutant, 2 jours)",
                "Formation avancée " + technologie + " (Expert, 3 jours)",
                "Mise en production avec " + technologie + " (DevOps, 1 jour)"
        );
    }
}