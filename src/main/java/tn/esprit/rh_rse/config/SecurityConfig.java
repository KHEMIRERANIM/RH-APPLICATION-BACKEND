package tn.esprit.rh_rse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tn.esprit.rh_rse.config.jwt.JwtAuthEntryPoint;
import tn.esprit.rh_rse.config.jwt.JwtFilter;

@Configuration("formationSecurityConfig")
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())  // Utilise la configuration CORS globale
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS requests (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/formations/disponibles").permitAll()
                        .requestMatchers("/api/formations/search").permitAll()

                        // ==================== AJOUT: FORMATEUR ENDPOINTS ====================
                        .requestMatchers(HttpMethod.GET, "/api/formateurs").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.GET, "/api/formateurs/**").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.POST, "/api/formateurs").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.PUT, "/api/formateurs/**").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.DELETE, "/api/formateurs/**").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers("/api/formateurs/*/formations/**").hasRole("ADMIN")

                        // Document endpoints
                        .requestMatchers(HttpMethod.POST, "/api/formateurs/*/formations/*/documents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/formateurs/formations/*/documents").hasAnyRole("ADMIN", "EMPLOYE","")
                        .requestMatchers(HttpMethod.DELETE, "/api/formateurs/documents/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/documents/download/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET, "/api/documents/**").permitAll()  // AJOUTÉ pour les documents
                        .requestMatchers(HttpMethod.POST, "/api/documents/**").permitAll() // AJOUTÉ pour les documents
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**").permitAll() // AJOUTÉ pour les documents

                        // Examen endpoints
                        .requestMatchers(HttpMethod.POST, "/api/formateurs/examens").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/formateurs/examens/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/formateurs/formations/*/examens").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.DELETE, "/api/formateurs/examens/*").hasRole("ADMIN")

                        // Validation endpoints
                        .requestMatchers(HttpMethod.PUT, "/api/formateurs/inscriptions/*/valider").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/formateurs/inscriptions/*/evaluer").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/formateurs/inscriptions/*/examens/*/soumettre").hasRole("EMPLOYE")
                        // ==================== FIN AJOUT ====================

                        // Admin only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")
                        .requestMatchers("/api/users/role/*").hasRole("ADMIN")
                        .requestMatchers("/api/users/department/*").hasRole("ADMIN")

                        // Users can access their own data
                        .requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/password").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/deactivate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/reactivate").hasRole("ADMIN")

                        // Formation endpoints
                        .requestMatchers(HttpMethod.GET, "/api/formations/disponibles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/formations/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/formations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/formations/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/formations/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/formations/*/toggle-active").hasRole("ADMIN")

                        // Inscription endpoints
                        .requestMatchers(HttpMethod.POST, "/api/formations/*/inscrire/*").hasAnyRole("EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/formations/employe/*/inscriptions").hasAnyRole("EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/formations/inscriptions/*/presence").hasAnyRole("EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/formations/inscriptions/*").hasAnyRole("EMPLOYE","FORMATEUR")

                        // QR Code endpoint
                        .requestMatchers(HttpMethod.GET, "/api/formations/*/qrcode").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        // Examen endpoints
                        .requestMatchers(HttpMethod.GET, "/api/examens/**").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/examens").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/examens/*/soumettre").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/examens/**").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/examens/**").hasAnyRole("ADMIN", "EMPLOYE","FORMATEUR")
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}