package com.craftbid.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.craftbid.security.ratelimit.RateLimitingFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Angular frontend
        configuration.setAllowedOrigins(
                List.of("http://localhost:4200", "http://127.0.0.1:4200")
        );

        // Allowed HTTP methods
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS",
                        "PATCH"
                )
        );

        // Allow all headers
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Expose Authorization header
        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // Allow credentials
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Disable CSRF for stateless REST
                .csrf(csrf -> csrf.disable())

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable form login and basic auth
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorize HTTP requests
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Static uploaded files
                        .requestMatchers("/uploads/**").permitAll()

                        // Public Auth APIs
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-registration",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification-otp",
                                "/api/auth/login",
                                "/api/auth/admin/send-otp",
                                "/api/auth/admin/verify-otp",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/test-email"
                        ).permitAll()

                        // Authenticated Seller / Artisan enable
                        .requestMatchers("/api/auth/enable-seller").authenticated()
                        .requestMatchers("/api/artisan/**").authenticated()

                        // Admin APIs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Category APIs
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // Craft Public Browsing
                        .requestMatchers(HttpMethod.GET, "/api/crafts/**").permitAll()

                        // Craft Modifications (require CUSTOMER or ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/crafts/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/crafts/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/crafts/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Craft Reels
                        .requestMatchers(HttpMethod.GET, "/api/craft-reels/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/craft-reels/craft/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/craft-reels/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/craft-reels/*/like").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/craft-reels/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/craft-reels/**").authenticated()

                        // Auction & Bids Public Browsing
                        .requestMatchers(HttpMethod.GET, "/api/auctions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions/{id:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions/{id:[0-9]+}/bids").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions/{id:[0-9]+}/participants").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions/{id:[0-9]+}/order").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions/craft/**").permitAll()

                        // Auction & Bids Authenticated Operations
                        .requestMatchers("/api/auctions/my-auctions").authenticated()
                        .requestMatchers("/api/auctions/my-bids").authenticated()
                        .requestMatchers("/api/auctions/artisan-orders").authenticated()
                        .requestMatchers("/api/auctions/buyer-orders").authenticated()
                        .requestMatchers("/api/bids/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auctions/**").authenticated()

                        // Payment API
                        .requestMatchers("/api/payments/**").authenticated()

                        // Support & Customer Service API
                        .requestMatchers(HttpMethod.POST, "/api/support/ticket").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/support/ticket/**").permitAll()
                        .requestMatchers("/api/support/**").authenticated()

                        // Social & Follow APIs
                        .requestMatchers(HttpMethod.GET, "/api/follow/count/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follow/status/**").permitAll()
                        .requestMatchers("/api/follow/**").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Add Rate Limiting filter after JWT filter
                .addFilterAfter(
                        rateLimitingFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}