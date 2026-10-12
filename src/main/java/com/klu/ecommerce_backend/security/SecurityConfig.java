
package com.klu.ecommerce_backend.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // Allow CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Signup and signin do not require authentication
                .requestMatchers("/api/auth/**").permitAll()

                // Payment webhook is called by the payment provider
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/payment/webhook"
                ).permitAll()

                // Product listing and viewing require login
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/products",
                    "/api/products/**"
                ).authenticated()

                // Only ADMIN can create products
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/products",
                    "/api/products/**"
                ).hasRole("ADMIN")

                // Only ADMIN can update products
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/products",
                    "/api/products/**"
                ).hasRole("ADMIN")

                // Only ADMIN can delete products
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/products",
                    "/api/products/**"
                ).hasRole("ADMIN")

                // Only ADMIN can access background jobs
                .requestMatchers("/api/jobs/**").hasRole("ADMIN")

                // All remaining endpoints require login
                .anyRequest().authenticated()
            )

            // JWT authentication is stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Local frontend and deployed frontend
        configuration.setAllowedOriginPatterns(
            List.of(
                "http://localhost:5173",
                "http://localhost:5177",
                "https://*.onrender.com"
            )
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
            )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
