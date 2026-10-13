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

    private static final String PRODUCTS = "/api/products";
    private static final String PRODUCTS_PATH = "/api/products/**";
    private static final String ADMIN = "ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth -> auth

                // Allow browser preflight requests.
                .requestMatchers(
                    HttpMethod.OPTIONS, "/**"
                ).permitAll()

                // Signup and signin.
                .requestMatchers(
                    "/api/auth/**"
                ).permitAll()

                // Payment provider webhook.
                .requestMatchers(
                    HttpMethod.POST, "/api/payment/webhook"
                ).permitAll()

                // Swagger UI and OpenAPI documentation.
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Authenticated users can view products.
                .requestMatchers(
                    HttpMethod.GET, PRODUCTS, PRODUCTS_PATH
                ).authenticated()

                // Only admins can create products.
                .requestMatchers(
                    HttpMethod.POST, PRODUCTS, PRODUCTS_PATH
                ).hasRole(ADMIN)

                // Only admins can update products.
                .requestMatchers(
                    HttpMethod.PUT, PRODUCTS, PRODUCTS_PATH
                ).hasRole(ADMIN)

                .requestMatchers(
                    HttpMethod.PATCH, PRODUCTS, PRODUCTS_PATH
                ).hasRole(ADMIN)

                // Only admins can delete products.
                .requestMatchers(
                    HttpMethod.DELETE, PRODUCTS, PRODUCTS_PATH
                ).hasRole(ADMIN)

                // Only admins can access background job endpoints.
                .requestMatchers(
                    "/api/jobs/**"
                ).hasRole(ADMIN)

                // Other endpoints require authentication.
                .anyRequest().authenticated()
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

        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://localhost:5177",
            "https://*.onrender.com"
        ));

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
