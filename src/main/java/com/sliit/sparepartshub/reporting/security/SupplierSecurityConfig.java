package com.sliit.sparepartshub.reporting.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Suppliers are NOT in the users table (see schema.sql - the role ENUM
 * only covers the 5 internal staff roles), so they can't share
 * SecurityConfig's authentication provider or UserDetailsService. This is
 * a second, fully separate filter chain scoped to /supplier-portal/**,
 * with its own login page and its own DaoAuthenticationProvider backed by
 * SupplierUserDetailsService.
 *
 * @Order(1) makes Spring Security check this chain's securityMatcher
 * first. Any request under /supplier-portal/** is handled entirely here
 * and never reaches SecurityConfig's staff chain (which is @Order(2)).
 * Everything outside /supplier-portal/** falls through to the staff chain
 * as before - no changes needed there beyond adding that @Order.
 */
@Configuration
public class SupplierSecurityConfig {

    private final SupplierUserDetailsService supplierUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SupplierSecurityConfig(SupplierUserDetailsService supplierUserDetailsService,
                                   PasswordEncoder passwordEncoder) {
        this.supplierUserDetailsService = supplierUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider supplierAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(supplierUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain supplierFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/supplier-portal/**")
            .authenticationProvider(supplierAuthenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/supplier-portal/login", "/supplier-portal/css/**").permitAll()
                .anyRequest().hasRole("SUPPLIER")
            )
            .formLogin(form -> form
                .loginPage("/supplier-portal/login")
                .loginProcessingUrl("/supplier-portal/login")
                .defaultSuccessUrl("/supplier-portal/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/supplier-portal/logout")
                .logoutSuccessUrl("/supplier-portal/login?logout")
                .permitAll()
            );
        return http.build();
    }
}
