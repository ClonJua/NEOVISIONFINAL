package com.neovision.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;


@Service
@Schema(description = "Genera y valida tokens JWT para autenticación y autorización. HMAC-SHA256 con la clave definida en application.properties")
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    private static final int LONGITUD_MINIMA_SECRETO = 32;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {

        if (secret == null || secret.trim().length() < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos " + LONGITUD_MINIMA_SECRETO
                    + " caracteres. Configuralo con un valor aleatorio fuerte via la variable de entorno JWT_SECRET"
                    + " (nunca dejes el valor de desarrollo en un despliegue real).");
        }
        // SHA-256 garantiza exactamente 256 bits sin importar el largo del input
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.secretKey = Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
        this.expiration = expiration;
    }

    /**
     * Genera un token JWT con el correo y rol del usuario.
     *
     * @param correo correo del usuario autenticado
     * @param rol    rol del usuario (ej: ROLE_ADMIN)
     * @return token JWT firmado con expiración de 24h
     */
    public String generateToken(String correo, String rol) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(correo)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el correo del usuario desde el token.
     *
     * @param token token JWT
     * @return correo del usuario
     */
    public String extractCorreo(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extrae el rol del usuario desde el token.
     *
     * @param token token JWT
     * @return rol del usuario (ej: ROLE_ADMIN)
     */
    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    /**
     * Extrae el identificador único (jti) del token, usado para revocarlo
     * individualmente al cerrar sesión sin necesidad de invalidar los demás
     * tokens activos del mismo usuario.
     *
     * @param token token JWT
     * @return identificador único del token
     */
    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * Extrae la fecha de expiración del token, usada para saber hasta cuándo
     * debe recordarse su revocación en {@link TokenBlacklistService}.
     *
     * @param token token JWT
     * @return fecha de expiración
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Verifica si el token es válido (firma correcta y no expirado).
     *
     * @param token token JWT
     * @return true si es válido, false si está expirado o es inválido
     */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
