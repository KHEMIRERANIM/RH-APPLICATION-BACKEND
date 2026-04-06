package tn.esprit.rh_rse.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader("Authorization");

        System.out.println("🌐 Requête: " + method + " " + path);
        System.out.println("🔑 Authorization header: " + (authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "NULL"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            System.out.println("📝 Token (50 chars): " + token.substring(0, Math.min(50, token.length())));

            boolean isValid = jwtUtil.isTokenValid(token);
            System.out.println("✅ Token valide: " + isValid);

            if (isValid) {
                String email = jwtUtil.extractEmail(token);
                String role  = jwtUtil.extractRole(token);

                System.out.println("👤 Email: " + email);
                System.out.println("🎭 Role: " + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                token,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("🔓 Authentication set OK");
            } else {
                System.out.println("❌ Token INVALIDE — authentication non définie");
            }
        } else {
            System.out.println("⚠️ Pas de token Bearer trouvé");
        }

        filterChain.doFilter(request, response);
    }
}