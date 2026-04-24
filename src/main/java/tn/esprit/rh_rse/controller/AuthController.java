package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.dto.request.LoginRequest;
import tn.esprit.rh_rse.dto.request.CreateUserRequest;
import tn.esprit.rh_rse.dto.response.AuthResponse;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.exception.UserNotFoundException;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

//@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "Email ou mot de passe incorrect"
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        if (user.getStatus() == tn.esprit.rh_rse.entity.enums.UserStatus.INACTIF) {
            throw new RuntimeException("Compte désactivé, contactez l'administrateur");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole())
                .poste(user.getPoste())
                .build());
    }

    @PostMapping("/register-candidate")
    public ResponseEntity<UserResponse> registerCandidate(@RequestBody CreateUserRequest request) {
        request.setRole(Role.CANDIDAT);
        return ResponseEntity.ok(userService.createUser(request));
    }
}