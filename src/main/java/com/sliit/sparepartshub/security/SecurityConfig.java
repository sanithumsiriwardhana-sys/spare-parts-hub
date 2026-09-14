package com.sliit.sparepartshub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for internal staff users.
 *
 * Supplier portal authentication is intentionally handled separately by
 * reporting.security.SupplierSecurityConfig.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * Internal staff security chain.
     *
     * SupplierSecurityConfig uses @Order(1) for /supplier-portal/**.
     * This chain therefore uses @Order(2).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth

                        // Public/static resources
                        .requestMatchers(
                                "/",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // Function 1 - Inventory Storage Location Tracking
                        .requestMatchers("/inventory/**")
                        .hasAnyRole(
                                "WAREHOUSE_CLERK",
                                "SALES_EXEC",
                                "ADMIN"
                        )

                        // Function 2 - Sales / POS
                        .requestMatchers("/sales/**")
                        .hasAnyRole(
                                "SALES_EXEC",
                                "ADMIN"
                        )

                        /*
                         * More specific stock-request rule must come before
                         * /stockmonitoring/** because Spring Security evaluates
                         * requestMatchers from top to bottom.
                         */
                        .requestMatchers("/stockmonitoring/stock-requests/**")
                        .hasAnyRole(
                                "SALES_EXEC",
                                "INVENTORY_SUPERVISOR",
                                "ADMIN"
                        )

                        // Function 3 - Urgency / Stock Monitoring
                        .requestMatchers("/stockmonitoring/**")
                        .hasAnyRole(
                                "INVENTORY_SUPERVISOR",
                                "ADMIN"
                        )

                        // Function 4 - Warranty / RMA
                        .requestMatchers("/warranty/**")
                        .hasAnyRole(
                                "OPERATIONS_COORDINATOR",
                                "ADMIN"
                        )

                        // Function 5 - Supplier Management
                        .requestMatchers("/supplier/**")
                        .hasRole("ADMIN")

                        // Function 6 - Reporting / Audit
                        .requestMatchers("/reporting/**")
                        .hasRole("ADMIN")

                        // Any other page requires authentication
                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
