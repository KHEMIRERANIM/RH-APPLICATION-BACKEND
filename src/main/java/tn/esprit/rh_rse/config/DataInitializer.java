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

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@entreprise.tn")) {
            User admin = User.builder()
                    .nom("Admin")
                    .prenom("Principal")
                    .email("admin@entreprise.tn")
                    .password(passwordEncoder.encode("admin123"))
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
}