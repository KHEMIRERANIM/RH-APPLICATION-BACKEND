package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Formation.Avis;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.AvisRepository;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvisFormationService {

    private final AvisRepository avisRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Avis ajouterAvis(String formationId, String employeId, Integer note, String titre, String commentaire) {
        log.info("📝 Ajout avis - Formation: {}, Employé: {}, Note: {}", formationId, employeId, note);

        if (avisRepository.existsByFormationIdAndEmployeId(formationId, employeId)) {
            throw new RuntimeException("Vous avez déjà donné votre avis pour cette formation");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        User employe = userRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        Avis avis = Avis.builder()
                .formationId(formationId)
                .employeId(employeId)
                .employeNom(employe.getNom())
                .employePrenom(employe.getPrenom())
                .employeEmail(employe.getEmail())
                .note(note)
                .titre(titre)
                .commentaire(commentaire)
                .valide(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Avis savedAvis = avisRepository.save(avis);
        miseAJourNoteMoyenne(formationId);

        log.info("✅ Avis ajouté avec succès - ID: {}", savedAvis.getId());
        return savedAvis;
    }

    @Transactional
    public Avis validerAvis(String avisId) {
        log.info("✅ Validation avis - ID: {}", avisId);

        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        avis.setValide(true);
        avis.setUpdatedAt(LocalDateTime.now());

        Avis savedAvis = avisRepository.save(avis);
        miseAJourNoteMoyenne(avis.getFormationId());

        return savedAvis;
    }

    @Transactional
    public void supprimerAvis(String avisId) {
        log.info("🗑️ Suppression avis - ID: {}", avisId);

        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        String formationId = avis.getFormationId();
        avisRepository.deleteById(avisId);
        miseAJourNoteMoyenne(formationId);
    }

    private void miseAJourNoteMoyenne(String formationId) {
        List<Avis> avisValides = avisRepository.findByFormationIdAndValide(formationId, true);

        if (avisValides.isEmpty()) {
            formationRepository.updateNoteMoyenne(formationId, 0.0);
            return;
        }

        int somme = 0;
        for (Avis avis : avisValides) {
            somme += avis.getNote();
        }
        double moyenne = (double) somme / avisValides.size();

        formationRepository.updateNoteMoyenne(formationId, moyenne);
        log.info("📊 Note moyenne mise à jour pour formation {}: {}/5", formationId, moyenne);
    }

    // ========== MÉTHODES POUR LE SENTIMENT ==========

    public List<Avis> getAllAvis() {
        log.info("📋 Récupération de tous les avis validés");
        return avisRepository.findAllValidAvis();
    }

    public List<Avis> getAllAvisForAnalysis() {
        log.info("📋 Récupération de tous les avis pour analyse");
        return avisRepository.findAllOrderByCreatedAtDesc();
    }

    public List<Avis> getAvisByFormationId(String formationId) {
        log.info("📋 Récupération des avis validés pour formation: {}", formationId);
        return avisRepository.findValidAvisByFormationIdOrderByCreatedAtDesc(formationId);
    }

    public List<Avis> getAvisByFormation(String formationId) {
        log.info("📋 Récupération des avis pour formation: {}", formationId);
        return avisRepository.findByFormationIdOrderByCreatedAtDesc(formationId);
    }

    public List<Avis> getAvisByEmploye(String employeId) {
        log.info("📋 Récupération des avis pour employé: {}", employeId);
        return avisRepository.findByEmployeId(employeId);
    }

    public Avis getAvisById(String avisId) {
        log.info("📋 Récupération de l'avis: {}", avisId);
        Optional<Avis> avis = avisRepository.findById(avisId);
        return avis.orElse(null);
    }

    public Avis getAvisByFormationAndEmploye(String formationId, String employeId) {
        return avisRepository.findByFormationIdAndEmployeId(formationId, employeId)
                .orElse(null);
    }

    public List<Avis> getAvisEnAttente() {
        log.info("📋 Récupération des avis en attente de validation");
        return avisRepository.findByValideFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public Avis updateAvis(Avis avis) {
        log.info("✏️ Mise à jour de l'avis: {}", avis.getId());
        avis.setUpdatedAt(LocalDateTime.now());
        Avis updated = avisRepository.save(avis);
        miseAJourNoteMoyenne(avis.getFormationId());
        return updated;
    }

    @Transactional
    public Avis updateAvisNote(String avisId, Integer nouvelleNote) {
        log.info("✏️ Mise à jour de la note de l'avis: {} -> {}", avisId, nouvelleNote);

        Avis avis = getAvisById(avisId);
        if (avis == null) {
            throw new RuntimeException("Avis non trouvé");
        }

        avis.setNote(nouvelleNote);
        avis.setUpdatedAt(LocalDateTime.now());
        Avis updated = avisRepository.save(avis);
        miseAJourNoteMoyenne(avis.getFormationId());

        return updated;
    }

    public boolean aDejaAvis(String formationId, String employeId) {
        return avisRepository.existsByFormationIdAndEmployeId(formationId, employeId);
    }

    // ========== STATISTIQUES ==========

    public Map<String, Object> getStatistiquesAvis(String formationId) {
        Map<String, Object> stats = new HashMap<>();

        List<Avis> avisValides = avisRepository.findByFormationIdAndValide(formationId, true);

        int totalAvis = avisValides.size();
        if (totalAvis == 0) {
            stats.put("totalAvis", 0);
            stats.put("moyenne", 0.0);
            stats.put("repartition", new HashMap<Integer, Long>());
            return stats;
        }

        int sommeNotes = 0;
        for (Avis avis : avisValides) {
            sommeNotes += avis.getNote();
        }
        double moyenne = (double) sommeNotes / totalAvis;

        Map<Integer, Long> repartition = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            long count = avisRepository.countByFormationIdAndNote(formationId, i);
            repartition.put(i, count);
        }

        stats.put("totalAvis", totalAvis);
        stats.put("moyenne", Math.round(moyenne * 10.0) / 10.0);
        stats.put("repartition", repartition);

        return stats;
    }

    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Avis> allAvis = getAllAvis();
        int total = allAvis.size();

        if (total == 0) {
            stats.put("totalAvis", 0);
            stats.put("moyenneGlobale", 0.0);
            return stats;
        }

        int somme = 0;
        Map<Integer, Integer> repartitionNotes = new HashMap<>();

        for (Avis avis : allAvis) {
            somme += avis.getNote();
            repartitionNotes.put(avis.getNote(), repartitionNotes.getOrDefault(avis.getNote(), 0) + 1);
        }

        stats.put("totalAvis", total);
        stats.put("moyenneGlobale", Math.round((double) somme / total * 10.0) / 10.0);
        stats.put("repartitionNotes", repartitionNotes);

        return stats;
    }
}