package com.neovision.auth.jwt;

import com.neovision.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que se ejecuta en cada petición HTTP.
 * Extrae el token JWT del header Authorization, lo valida y registra
 * al usuario en el contexto de seguridad de Spring si es válido.
 * Si no hay token o es inválido, deja pasar la petición sin autenticar.
 */
@Component
@Schema(description = "Todo el tema de seguridad, como el filtro de autenticacion, se maneja aqui.")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Intercepta cada request, extrae el Bearer token y autentica al usuario.
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP
     * @param filterChain cadena de filtros de Spring Security
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token) && !tokenBlacklistService.estaRevocado(jwtService.extractJti(token))) {
            String correo = jwtService.extractCorreo(token);
            String rol = jwtService.extractRol(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            correo,
                            null,
                            List.of(new SimpleGrantedAuthority(rol))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
