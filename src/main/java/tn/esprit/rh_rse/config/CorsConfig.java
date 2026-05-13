package tn.esprit.rh_rse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://*.ngrok-free.dev",
                        "https://*.ngrok-free.app",
                        "https://*.ngrok.io",
                        "https://*.trycloudflare.com",
                        "http://10.0.0.*",
                        "http://192.168.*.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
