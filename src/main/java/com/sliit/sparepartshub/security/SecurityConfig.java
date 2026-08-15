package com.sliit.sparepartshub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Route protection is scoped by URL prefix, one prefix per function
 * package (see the /inventory, /sales, /stockmonitoring, /warranty,
 * /supplier, /reporting packages). Admin can reach everything since the
 * Shop Owner/Admin role oversees the whole system per the proposal.
 *
 * Update the requestMatchers below as each member builds out their
 * controllers - these paths are placeholders matching the six function
 * packages, not final routes.
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
        // Matches password_hash VARCHAR(60) in the schema - that column
        // width is sized specifically for BCrypt hashes.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 6.3+ removed the no-arg constructor + setUserDetailsService()
        // pattern - UserDetailsService is now passed directly into the constructor.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/webjars/**").permitAll()
                .requestMatchers("/inventory/**").hasAnyRole("WAREHOUSE_CLERK", "SALES_EXEC", "ADMIN")
                .requestMatchers("/sales/**").hasAnyRole("SALES_EXEC", "ADMIN")
                .requestMatchers("/stockmonitoring/**").hasAnyRole("INVENTORY_SUPERVISOR", "ADMIN")
                .requestMatchers("/warranty/**").hasAnyRole("OPERATIONS_COORDINATOR", "ADMIN")
                .requestMatchers("/supplier/**", "/reporting/**").hasRole("ADMIN")
                .anyRequest().authenticated()
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
