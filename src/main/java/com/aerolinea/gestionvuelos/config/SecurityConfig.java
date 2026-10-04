package com.aerolinea.gestionvuelos.config;

import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final UsuarioRepository usuarioRepository;

  public SecurityConfig(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(
                        "/", "/inicio", "/login", "/registro", "/registro/**", "/acceso-denegado")
                    .permitAll()
                    .requestMatchers("/pasajero/**")
                    .hasRole("PASAJERO")
                    .requestMatchers("/empleado/**")
                    .hasRole("EMPLEADO")
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            login ->
                login
                    .loginPage("/login")
                    .usernameParameter("email")
                    .successHandler(authenticationSuccessHandler())
                    .failureHandler(authenticationFailureHandler())
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
        .sessionManagement(
            session ->
                session
                    .invalidSessionUrl("/login?expired")
                    .sessionFixation(sessionFixation -> sessionFixation.migrateSession()))
        .exceptionHandling(exception -> exception.accessDeniedPage("/acceso-denegado"));
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    final InMemoryUserDetailsManager fallbackUsers = createFallbackUsers();
    return username -> {
      final String normalizedUsername =
          username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
      final UserDetails fallbackUser = loadFallbackUser(fallbackUsers, normalizedUsername);
      if (fallbackUser != null) {
        return fallbackUser;
      }
      return usuarioRepository
          .findByEmailIgnoreCase(normalizedUsername)
          .map(this::toUserDetails)
          .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    };
  }

  private org.springframework.security.web.authentication.AuthenticationFailureHandler
      authenticationFailureHandler() {
    return (request, response, exception) -> response.sendRedirect("/login?error");
  }

  private org.springframework.security.web.authentication.AuthenticationSuccessHandler
      authenticationSuccessHandler() {
    return (request, response, authentication) -> {
      final String targetPath =
          RolUsuario.fromAuthorities(authentication.getAuthorities())
              .map(RolUsuario::getHomePath)
              .orElse("/");
      response.sendRedirect(targetPath);
    };
  }

  private UserDetails loadFallbackUser(InMemoryUserDetailsManager fallbackUsers, String username) {
    try {
      return fallbackUsers.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
      return null;
    }
  }

  private UserDetails toUserDetails(Usuario usuario) {
    return User.builder()
        .username(usuario.getEmail())
        .password(usuario.getPasswordHash())
        .disabled(!usuario.getActivo())
        .authorities(Arrays.asList(new SimpleGrantedAuthority(usuario.getRol().getAuthority())))
        .build();
  }

  private InMemoryUserDetailsManager createFallbackUsers() {
    final UserDetails admin =
        User.builder()
            .username("admin@aerolinea.local")
            .password(passwordEncoder().encode("admin123"))
            .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build();

    final UserDetails passenger =
        User.builder()
            .username("pasajero@aerolinea.local")
            .password(passwordEncoder().encode("pass123"))
            .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_PASAJERO")))
            .build();

    final UserDetails employee =
        User.builder()
            .username("empleado@aerolinea.local")
            .password(passwordEncoder().encode("emp123"))
            .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_EMPLEADO")))
            .build();

    return new InMemoryUserDetailsManager(admin, passenger, employee);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
