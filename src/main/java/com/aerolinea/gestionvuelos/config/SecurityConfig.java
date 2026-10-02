package com.aerolinea.gestionvuelos.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/", "/pasajero", "/empleado").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(login -> login
            .loginPage("/login")
            .permitAll()
        )
        .logout(logout -> logout.permitAll());
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    UserDetails admin = User.builder()
        .username("admin")
        .password(passwordEncoder().encode("admin123"))
        .authorities(Arrays.asList(
            new SimpleGrantedAuthority("ROLE_ADMIN")
        ))
        .build();

    UserDetails passenger = User.builder()
        .username("pasajero")
        .password(passwordEncoder().encode("pass123"))
        .authorities(Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER")
        ))
        .build();

    UserDetails employee = User.builder()
        .username("empleado")
        .password(passwordEncoder().encode("emp123"))
        .authorities(Arrays.asList(
            new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        ))
        .build();

    return new InMemoryUserDetailsManager(admin, passenger, employee);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
