package com.neovision.auth.service;

import com.neovision.auth.dto.AuthResponse;
import com.neovision.auth.dto.LoginRequest;
import com.neovision.auth.dto.MicrosoftLoginRequest;
import com.neovision.auth.dto.RegistroRequest;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Servicio de autenticación del sistema NeoVision.
 * Gestiona el login con credenciales propias, registro de pacientes
 * y autenticación institucional mediante Microsoft MSAL.
 */
@Schema(description = "Genera los metodos a usar en el implementacion de AuthServiceImpl, como el login, registro y login con Microsoft.")
public interface AuthService {

    /**
     * Autentica un usuario con correo y contraseña.
     *
     * @param request credenciales del usuario (correo + contraseña)
     * @return token JWT con el rol y correo del usuario autenticado
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registra un nuevo paciente en el sistema.
     * Valida el formato de teléfono, tipo de documento y edad según el documento.
     *
     * @param request datos del paciente (nombre, documento, correo, contraseña, etc.)
     * @return token JWT con el rol ROLE_PACIENTE y correo del nuevo usuario
     */
    AuthResponse registro(RegistroRequest request);

    /**
     * Autentica un usuario mediante su cuenta institucional de Microsoft.
     * Valida el accessToken con la API de Microsoft Graph y obtiene el correo.
     *
     * @param request accessToken generado por MSAL en el frontend
     * @return token JWT con el rol y correo del usuario institucional
     */
    AuthResponse loginMicrosoft(MicrosoftLoginRequest request);

    /**
     * Revoca el token del header {@code Authorization} recibido, para que
     * {@code JwtAuthenticationFilter} lo rechace en cualquier petición
     * posterior aunque su firma y expiración sigan siendo válidas.
     * Idempotente: si el header falta o el token ya es inválido, no lanza error.
     *
     * @param authHeader valor crudo del header Authorization (p. ej. "Bearer eyJ...")
     */
    void logout(String authHeader);
}
