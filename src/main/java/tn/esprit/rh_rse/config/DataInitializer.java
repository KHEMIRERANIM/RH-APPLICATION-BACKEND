package tn.esprit.rh_rse.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

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
            System.out.println("Admin cree : admin@entreprise.tn / admin123");
        } else {
            System.out.println("Admin existe deja - aucune action necessaire");
        }
    }
}