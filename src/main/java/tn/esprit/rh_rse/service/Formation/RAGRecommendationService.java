package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.dto.Formation.RecommandationPersonnaliseeDTO;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGRecommendationService {

    private final TechWatchService techWatchService;
    private final FormationService formationService;

    // Index Lucene en mémoire
    private Directory directory = new ByteBuffersDirectory();
    private StandardAnalyzer analyzer = new StandardAnalyzer();
    private boolean indexed = false;

    /**
     * Indexe toutes les formations avec Lucene
     */
    public void indexerFormations() {
        try {
            directory = new ByteBuffersDirectory();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(directory, config);

            List<FormationDTO> formations = formationService.getAllFormations();
            for (FormationDTO f : formations) {
                Document doc = new Document();
                doc.add(new StringField("id", f.getId(), Field.Store.YES));
                doc.add(new TextField("titre", f.getTitre() != null ? f.getTitre() : "", Field.Store.YES));
                doc.add(new TextField("description", f.getDescription() != null ? f.getDescription() : "", Field.Store.YES));
                doc.add(new TextField("objectifs", f.getObjectifs() != null ? f.getObjectifs() : "", Field.Store.YES));
                // Champ combiné pour la recherche globale
                String full = f.getTitre() + " " + f.getDescription() + " " + f.getObjectifs();
                doc.add(new TextField("contenu", full, Field.Store.NO));
                writer.addDocument(doc);
            }

            writer.close();
            indexed = true;
            log.info(" {} formations indexées avec Lucene", formations.size());

        } catch (IOException e) {
            log.error("Erreur indexation Lucene: {}", e.getMessage());
        }
    }

    /**
     * Recherche sémantique avec Lucene (TF-IDF)
     */
    private List<FormationDTO> rechercherFormations(String requete) {
        if (!indexed) indexerFormations();

        List<FormationDTO> resultats = new ArrayList<>();
        try {
            IndexReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Recherche sur plusieurs champs
            String[] fields = {"titre", "description", "objectifs", "contenu"};
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);

            // Escape les caractères spéciaux
            String requeteEscapee = QueryParser.escape(requete);
            org.apache.lucene.search.Query query = parser.parse(requeteEscapee);

            TopDocs topDocs = searcher.search(query, 5);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String id = doc.get("id");
                FormationDTO formation = formationService.getFormationById(id);
                if (formation != null) {
                    resultats.add(formation);
                }
            }

            reader.close();
        } catch (Exception e) {
            log.warn("Erreur recherche Lucene pour '{}': {}", requete, e.getMessage());
            // Fallback : recherche simple par contains
            resultats = formationService.getAllFormations().stream()
                    .filter(f -> {
                        String texte = (f.getTitre() + " " + f.getDescription()).toLowerCase();
                        return texte.contains(requete.toLowerCase());
                    })
                    .limit(3)
                    .collect(Collectors.toList());
        }

        return resultats;
    }

    /**
     * Recommandation personnalisée pour un employé
     */
    public RecommandationPersonnaliseeDTO recommanderPourEmploye(
            String employeId,
            List<ParticipantInscription> historique) {

        if (!indexed) indexerFormations();

        RecommandationPersonnaliseeDTO result = new RecommandationPersonnaliseeDTO();
        result.setEmployeId(employeId);

        // 1. Technologies connues depuis l'historique
        List<String> technologiesConnues = extraireTechnologiesDeHistorique(historique);
        result.setTechnologiesConnues(technologiesConnues);
        log.info("📊 Technologies connues pour {}: {}", employeId, technologiesConnues);

        // 2. Technologies tendances GitHub + ArXiv combinés
        List<String> technologiesTendances = new ArrayList<>(techWatchService.getGitHubTrends());

        List<Map<String, String>> papers = techWatchService.getArxivPapers();
        for (Map<String, String> paper : papers) {
            String texte = paper.getOrDefault("title", "") + " " + paper.getOrDefault("summary", "");
            List<String> techsDuPaper = techWatchService.extractTechnologies(texte);
            technologiesTendances.addAll(techsDuPaper);
        }

        technologiesTendances = technologiesTendances.stream()
                .distinct()
                .collect(Collectors.toList());

        log.info("🔥 {} technologies tendances détectées (GitHub + ArXiv)", technologiesTendances.size());

        // 3. Gap analysis — technologies tendances non maîtrisées par l'employé
        List<String> technologiesRecommandees = technologiesTendances.stream()
                .filter(tech -> technologiesConnues.stream()
                        .noneMatch(c -> c.toLowerCase().contains(tech.toLowerCase()) ||
                                tech.toLowerCase().contains(c.toLowerCase())))
                .limit(5)
                .collect(Collectors.toList());

        result.setTechnologiesRecommandees(technologiesRecommandees);
        log.info("🎯 Gap détecté pour {}: {}", employeId, technologiesRecommandees);

        // 4. Suggestions de formation via Lucene
        List<RecommandationPersonnaliseeDTO.FormationSuggestion> suggestions = new ArrayList<>();

        for (String tech : technologiesRecommandees) {
            List<FormationDTO> formationsTrouvees = rechercherFormations(tech);

            if (!formationsTrouvees.isEmpty()) {
                for (FormationDTO f : formationsTrouvees) {
                    RecommandationPersonnaliseeDTO.FormationSuggestion sugg =
                            new RecommandationPersonnaliseeDTO.FormationSuggestion();
                    sugg.setFormationId(f.getId());
                    sugg.setTitre(f.getTitre());
                    sugg.setDescription(f.getDescription());
                    sugg.setScore(85 + new Random().nextInt(10));
                    sugg.setRaison("Formation recommandée pour acquérir " + tech
                            + (estTendanceArxiv(tech, papers) ? " [source: ArXiv]" : " [source: GitHub]"));
                    suggestions.add(sugg);
                }
            } else {
                RecommandationPersonnaliseeDTO.FormationSuggestion sugg =
                        new RecommandationPersonnaliseeDTO.FormationSuggestion();
                sugg.setFormationId(null);
                sugg.setTitre("Nouvelle formation : " + tech);
                sugg.setDescription("Formation à créer sur " + tech
                        + " — technologie émergente "
                        + (estTendanceArxiv(tech, papers) ? "issue de la recherche scientifique" : "issue de GitHub Trending"));
                sugg.setScore(90);
                sugg.setRaison("Technologie tendance non disponible dans le catalogue");
                suggestions.add(sugg);
            }
        }

        result.setSuggestionsFormations(suggestions.stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(3)
                .collect(Collectors.toList()));

        // 5. Niveau recommandé
        result.setProchainNiveau(recommanderNiveau(technologiesConnues));

        log.info(" Recommandation générée pour {} — {} suggestions", employeId, result.getSuggestionsFormations().size());

        return result;
    }

    /**
     * Vérifie si une technologie vient des papers ArXiv
     */
    private boolean estTendanceArxiv(String tech, List<Map<String, String>> papers) {
        return papers.stream().anyMatch(paper -> {
            String texte = paper.getOrDefault("title", "") + " " + paper.getOrDefault("summary", "");
            return texte.toLowerCase().contains(tech.toLowerCase());
        });
    }
    /**
     * Extrait les technologies depuis l'historique de formations suivies
     */
    private List<String> extraireTechnologiesDeHistorique(List<ParticipantInscription> historique) {
        Set<String> technologies = new HashSet<>();

        if (historique == null || historique.isEmpty()) {
            log.warn("⚠️ Historique vide pour cet employé");
            return new ArrayList<>();
        }

        for (ParticipantInscription ins : historique) {
            try {
                FormationDTO formation = formationService.getFormationById(ins.getFormationId());
                if (formation != null) {
                    List<String> techs = techWatchService.extractTechnologies(
                            formation.getTitre() + " " + formation.getDescription());
                    technologies.addAll(techs);
                }
            } catch (Exception e) {
                log.warn("Erreur extraction historique: {}", e.getMessage());
            }
        }

        return new ArrayList<>(technologies);
    }

    private String recommanderNiveau(List<String> technologiesConnues) {
        if (technologiesConnues.size() < 3) return "DÉBUTANT";
        if (technologiesConnues.size() < 8) return "INTERMÉDIAIRE";
        return "EXPERT";
    }
}