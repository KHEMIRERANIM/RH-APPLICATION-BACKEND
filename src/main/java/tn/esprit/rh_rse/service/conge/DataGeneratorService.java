package tn.esprit.rh_rse.service.conge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.conge.DemandeConge;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.entity.enums.TypeConge;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.repository.conge.DemandeCongeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataGeneratorService {

    private final DemandeCongeRepository demandeRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public String genererDonneesSynthetiques() {
        log.info("=== DÉBUT GÉNÉRATION DONNÉES SYNTHÉTIQUES ===");

        List<User> employes = userRepository.findAll();
        if (employes.isEmpty()) {
            return "❌ Aucun employé trouvé dans la base";
        }

        // Filtrer seulement les employés (pas les admins)
        List<User> employesOnly = employes.stream()
                .filter(u -> u.getRole().name().equals("EMPLOYE"))
                .toList();

        if (employesOnly.isEmpty()) {
            return "❌ Aucun employé avec rôle EMPLOYE trouvé";
        }

        log.info("Employés trouvés: {}", employesOnly.size());

        int count = 0;

        // Générer des demandes pour 2022, 2023, 2024, 2025
        for (int annee = 2022; annee <= 2025; annee++) {
            for (User employe : employesOnly) {
                // 3 à 8 demandes par employé par an
                int nbDemandes = random.nextInt(6) + 3;

                for (int i = 0; i < nbDemandes; i++) {
                    DemandeConge demande = genererDemandeAleatoire(employe, annee);
                    demandeRepository.save(demande);
                    count++;
                }
            }
        }

        log.info("=== FIN - {} demandes générées ===", count);
        return "✅ " + count + " demandes de congé générées pour " + employesOnly.size() + " employés";
    }

    private DemandeConge genererDemandeAleatoire(User employe, int annee) {
        int mois = getMoisProbable();
        int jourDebut = random.nextInt(20) + 1;
        int duree = random.nextInt(12) + 3;

        // Éviter les dates invalides
        LocalDate dateDebut;
        try {
            dateDebut = LocalDate.of(annee, mois, Math.min(jourDebut, 28));
        } catch (Exception e) {
            dateDebut = LocalDate.of(annee, mois, 1);
        }
        LocalDate dateFin = dateDebut.plusDays(duree);

        StatutConge statut = getStatutProbable(mois);

        return DemandeConge.builder()
                .employeId(employe.getId())
                .managerId(employe.getManagerId())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .nombreJours(duree)
                .motif("Demande de congé " + getTypeCongeAleatoire())
                .type(getTypeCongeAleatoire())
                .statut(statut)
                .createdAt(LocalDateTime.now().minusMonths((2025 - annee) * 12 + random.nextInt(12)))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private int getMoisProbable() {
        // Juillet, Août, Décembre, Mai = plus probables
        int[] moisProbables = {7, 7, 7, 8, 8, 8, 12, 12, 12, 5, 5, 6, 4, 1, 2, 3, 9, 10, 11};
        return moisProbables[random.nextInt(moisProbables.length)];
    }

    private StatutConge getStatutProbable(int mois) {
        if (mois == 7 || mois == 8) {
            // En été, 75% approuvés
            return random.nextInt(100) < 75 ? StatutConge.APPROUVE : StatutConge.REFUSE;
        }
        // Sinon 85% approuvés
        return random.nextInt(100) < 85 ? StatutConge.APPROUVE : StatutConge.REFUSE;
    }

    private TypeConge getTypeCongeAleatoire() {
        TypeConge[] types = {TypeConge.CONGE_ANNUEL, TypeConge.CONGE_MALADIE,
                TypeConge.CONGE_SANS_SOLDE, TypeConge.CONGE_MATERNITE,
                TypeConge.CONGE_PATERNITE, TypeConge.AUTRE};
        return types[random.nextInt(types.length)];
    }
}
