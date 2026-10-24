package com.institute.ims.config;

import com.institute.ims.security.CustomUserDetailsService;
import com.institute.ims.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CustomUserDetailsService userDetailsService) {

        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                userDetailsService
        );

        provider.setPasswordEncoder(
                passwordEncoder()
        );

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Allow unauthenticated GET requests to departments so registration can load them
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/departments",
                                "/api/departments/**"
                        ).permitAll()

                        // Allow unauthenticated GET to curriculum endpoint for registration page
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/subjects/curriculum"
                        ).permitAll()

                        // Verification endpoints — ADMIN/HOD only
                        .requestMatchers(
                                "/api/users/pending"
                        ).hasAnyRole("ADMIN", "HOD")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/*/verify"
                        ).hasAnyRole("ADMIN", "HOD")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/verify-bulk"
                        ).hasAnyRole("ADMIN", "HOD")

                        .requestMatchers(
                                "/api/users/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/subjects/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/departments/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/attendance/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/assignments/**"
                        ).authenticated()

                        .anyRequest()
                        .authenticated()
                )


                .authenticationProvider(
                        authenticationProvider()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}