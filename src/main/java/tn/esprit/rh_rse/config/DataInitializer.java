package tn.esprit.rh_rse.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FormationRepository formationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeAdmin();
        initializeFormations();
    }

    private void initializeAdmin() {
        if (!userRepository.existsByEmail("admin@entreprise.tn")) {
            User admin = User.builder()
                    .nom("Admin")
                    .prenom("Principal")
                    .email("admin@entreprise.tn")
                    .password(passwordEncoder.encode("admin123"))
                    .telephone("00000000")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIF)
                    .departement("Direction")
                    .poste("Administrateur")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .dateEmbauche(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            log.info("✅ Admin créé : admin@entreprise.tn / admin123");
        } else {
            log.info("✅ Admin existe déjà — aucune action nécessaire");
        }
    }

    private void initializeFormations() {
        // Vérifier si la collection formations est vide
        if (formationRepository.count() == 0) {
            log.info("📚 Initialisation des données de formation...");

            // Formation 1: Java Spring Boot
            Formation formation1 = new Formation();
            formation1.setTitre("Formation Java Spring Boot");
            formation1.setDescription("Apprenez à développer des applications backend avec Spring Boot");
            formation1.setObjectifs("Maîtriser Spring Boot, créer des API REST, utiliser MongoDB");
            formation1.setPreRequis("Connaissances de base en Java");
            formation1.setType("TECHNIQUE");
            formation1.setDureeHeures(40);
            formation1.setNombrePlaces(20);
            formation1.setPlacesDisponibles(15);
            formation1.setNiveau("INTERMEDIAIRE");
            formation1.setFormateur("Jean Dupont");
            formation1.setFormateurBio("Expert Java avec 10 ans d'expérience");
            formation1.setLieu("En ligne");
            formation1.setLienVisio("https://meet.google.com/spring-boot");
            formation1.setDateDebut(LocalDateTime.now().plusDays(7));
            formation1.setDateFin(LocalDateTime.now().plusDays(11));
            formation1.setDateLimiteInscription(LocalDateTime.now().plusDays(3));
            formation1.setImageUrl("https://example.com/spring-boot.jpg");
            formation1.setActive(true);
            formation1.setCreatedAt(LocalDateTime.now());
            formation1.setUpdatedAt(LocalDateTime.now());

            // Formation 2: Gestion de Projet Agile
            Formation formation2 = new Formation();
            formation2.setTitre("Gestion de Projet Agile");
            formation2.setDescription("Formation sur les méthodologies agiles et Scrum");
            formation2.setObjectifs("Maîtriser Scrum, Kanban, et les rituels agiles");
            formation2.setPreRequis("Aucun");
            formation2.setType("MANAGERIAL");
            formation2.setDureeHeures(24);
            formation2.setNombrePlaces(15);
            formation2.setPlacesDisponibles(10);
            formation2.setNiveau("DEBUTANT");
            formation2.setFormateur("Marie Martin");
            formation2.setFormateurBio("Coach agile certifiée");
            formation2.setLieu("Présentiel - Salle A");
            formation2.setLienVisio(null);
            formation2.setDateDebut(LocalDateTime.now().plusDays(14));
            formation2.setDateFin(LocalDateTime.now().plusDays(17));
            formation2.setDateLimiteInscription(LocalDateTime.now().plusDays(7));
            formation2.setImageUrl("https://example.com/agile.jpg");
            formation2.setActive(true);
            formation2.setCreatedAt(LocalDateTime.now());
            formation2.setUpdatedAt(LocalDateTime.now());

            // Formation 3: RSE et Développement Durable
            Formation formation3 = new Formation();
            formation3.setTitre("RSE et Développement Durable");
            formation3.setDescription("Comprendre et mettre en œuvre la RSE en entreprise");
            formation3.setObjectifs("Maîtriser les enjeux RSE, calculer l'empreinte carbone");
            formation3.setPreRequis("Aucun");
            formation3.setType("RSE");
            formation3.setDureeHeures(16);
            formation3.setNombrePlaces(30);
            formation3.setPlacesDisponibles(25);
            formation3.setNiveau("DEBUTANT");
            formation3.setFormateur("Sophie Bernard");
            formation3.setFormateurBio("Consultante RSE");
            formation3.setLieu("En ligne");
            formation3.setLienVisio("https://meet.google.com/rse");
            formation3.setDateDebut(LocalDateTime.now().plusDays(21));
            formation3.setDateFin(LocalDateTime.now().plusDays(23));
            formation3.setDateLimiteInscription(LocalDateTime.now().plusDays(14));
            formation3.setImageUrl("https://example.com/rse.jpg");
            formation3.setActive(true);
            formation3.setCreatedAt(LocalDateTime.now());
            formation3.setUpdatedAt(LocalDateTime.now());

            // Formation 4: Angular pour les applications web modernes
            Formation formation4 = new Formation();
            formation4.setTitre("Angular pour les applications web modernes");
            formation4.setDescription("Développez des applications frontend avec Angular 17+");
            formation4.setObjectifs("Maîtriser Angular, créer des composants réutilisables, utiliser RxJS");
            formation4.setPreRequis("Connaissances de base en JavaScript/TypeScript");
            formation4.setType("TECHNIQUE");
            formation4.setDureeHeures(35);
            formation4.setNombrePlaces(25);
            formation4.setPlacesDisponibles(20);
            formation4.setNiveau("INTERMEDIAIRE");
            formation4.setFormateur("Pierre Lambert");
            formation4.setFormateurBio("Expert frontend avec 8 ans d'expérience");
            formation4.setLieu("En ligne");
            formation4.setLienVisio("https://meet.google.com/angular");
            formation4.setDateDebut(LocalDateTime.now().plusDays(10));
            formation4.setDateFin(LocalDateTime.now().plusDays(14));
            formation4.setDateLimiteInscription(LocalDateTime.now().plusDays(5));
            formation4.setImageUrl("https://example.com/angular.jpg");
            formation4.setActive(true);
            formation4.setCreatedAt(LocalDateTime.now());
            formation4.setUpdatedAt(LocalDateTime.now());

            // Formation 5: Leadership et Management d'équipe
            Formation formation5 = new Formation();
            formation5.setTitre("Leadership et Management d'équipe");
            formation5.setDescription("Développez vos compétences en leadership et gestion d'équipe");
            formation5.setObjectifs("Motiver son équipe, gérer les conflits, prendre des décisions");
            formation5.setPreRequis("Avoir au moins 2 ans d'expérience en management");
            formation5.setType("MANAGERIAL");
            formation5.setDureeHeures(30);
            formation5.setNombrePlaces(12);
            formation5.setPlacesDisponibles(8);
            formation5.setNiveau("AVANCE");
            formation5.setFormateur("Thomas Richard");
            formation5.setFormateurBio("Coach certifié en leadership");
            formation5.setLieu("Présentiel - Salle Executive");
            formation5.setLienVisio(null);
            formation5.setDateDebut(LocalDateTime.now().plusDays(28));
            formation5.setDateFin(LocalDateTime.now().plusDays(32));
            formation5.setDateLimiteInscription(LocalDateTime.now().plusDays(14));
            formation5.setImageUrl("https://example.com/leadership.jpg");
            formation5.setActive(true);
            formation5.setCreatedAt(LocalDateTime.now());
            formation5.setUpdatedAt(LocalDateTime.now());

            // Sauvegarder toutes les formations
            formationRepository.saveAll(Arrays.asList(
                    formation1,
                    formation2,
                    formation3,
                    formation4,
                    formation5
            ));

            log.info("✅ {} formations ont été créées avec succès", formationRepository.count());
        } else {
            log.info("📚 La collection formations existe déjà avec {} enregistrement(s)", formationRepository.count());
        }
    }
}