package tn.esprit.rh_rse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tn.esprit.rh_rse.config.jwt.JwtAuthEntryPoint;
import tn.esprit.rh_rse.config.jwt.JwtFilter;

@Configuration
@EnableWebSecurity
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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC ─────────────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/chatbot/**").permitAll()
                        // ── USERS (Admin seulement) ────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/users/role/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/users/department/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "EMPLOYE")

                        // ── SALAIRES ────────────────────────────────────────
                        // Admin peut tout faire
                        .requestMatchers(HttpMethod.POST, "/api/salaires").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/salaires/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/salaires/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/salaires").hasRole("ADMIN")
                        // 🔥 NOUVEAU : Employé peut voir ses bulletins
                        .requestMatchers(HttpMethod.GET,  "/api/salaires/employe/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET,  "/api/salaires/{id}").hasAnyRole("ADMIN", "EMPLOYE")

                        // ── CONGES MANAGER (Admin joue le role manager) ────
                        .requestMatchers("/api/conges/manager/**").hasRole("ADMIN")

                        // ── CONGES EMPLOYE (Employe + Admin) ──────────────
                        .requestMatchers(HttpMethod.POST,   "/api/conges").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET,    "/api/conges/employe/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET,    "/api/conges/solde/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET,    "/api/conges/{id}").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.DELETE, "/api/conges/**").hasAnyRole("ADMIN", "EMPLOYE")

                        // ── TOUT LE RESTE : authentifié ───────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}