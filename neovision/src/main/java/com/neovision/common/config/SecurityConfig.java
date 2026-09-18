package com.neovision.common.config;

import com.neovision.auth.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad HTTP del sistema.
 * Define rutas públicas, protege endpoints por rol y registra el filtro JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Solo se emite cuando la peticion ya llega por HTTPS (Azure App
                // Service la sirve asi en produccion); en local, sobre HTTP, el
                // navegador la ignora igual, asi que no hace falta condicionarla
                // por perfil. Confirmar tras desplegar con "https-only" activo
                // (ver DEPLOY.md) que el header realmente llega al cliente.
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Defensa en profundidad. connect-src incluye login.microsoftonline.com
                        // porque MSAL (login institucional) llama ese dominio directamente
                        // desde el navegador para intercambiar el codigo de Microsoft por un
                        // token — sin esto, default-src 'self' lo bloquea en silencio y el
                        // login con Microsoft nunca termina de resolverse.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; connect-src 'self' https://login.microsoftonline.com; "
                                        + "frame-ancestors 'none'; base-uri 'self'")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Cascaron estatico del frontend (solo aplica cuando se empaca junto
                        // al backend en un unico despliegue, ej. la demo). La pagina en si no
                        // necesita autenticacion: la proteccion real esta en las llamadas a
                        // /api/** que React hace despues de cargar, no en poder ver el HTML/JS.
                        .requestMatchers(
                                "/", "/index.html", "/assets/**", "/favicon.svg", "/icons.svg",
                                "/login", "/registro", "/dashboard", "/admin", "/estudiante", "/docente"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/docente/**").hasAuthority("ROLE_DOCENTE")
                        .requestMatchers("/api/estudiante/**").hasAuthority("ROLE_ESTUDIANTE")
                        .requestMatchers("/api/paciente/**").hasAuthority("ROLE_PACIENTE")
                        // Historia clínica: estudiante, docente y admin (secretaria).
                        // Las reglas finas (dueño / supervisor / solo lectura) se aplican en el servicio.
                        .requestMatchers("/api/historias/**").hasAnyAuthority("ROLE_ESTUDIANTE", "ROLE_DOCENTE", "ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
