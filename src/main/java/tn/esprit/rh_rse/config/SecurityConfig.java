package tn.esprit.rh_rse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tn.esprit.rh_rse.config.jwt.JwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Users — Admin only
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/role/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/department/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "EMPLOYE", "CANDIDAT")

                        // Mutuelle — tous les endpoints nécessitent authentification
                        .requestMatchers("/api/avantages/stats/**").hasRole("ADMIN")
                        .requestMatchers("/api/partenaires/**").authenticated()
                        .requestMatchers("/api/commentaires/**").authenticated()
                        .requestMatchers("/api/avantages-reservations/**").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // RESTAURATION — Ajoutez ici les endpoints de votre module
                        // Exemple :
                        // .requestMatchers("/api/menus/**").authenticated()
                        // .requestMatchers("/api/commandes/**").authenticated()
                        // .requestMatchers("/api/restaurants/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}