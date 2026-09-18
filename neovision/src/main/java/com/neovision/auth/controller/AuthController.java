package com.neovision.auth.controller;

import com.neovision.auth.dto.AuthResponse;
import com.neovision.auth.dto.LoginRequest;
import com.neovision.auth.dto.MicrosoftLoginRequest;
import com.neovision.auth.dto.RegistroRequest;
import com.neovision.auth.service.AuthService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Endpoints de login y registro. Accesibles sin autenticación previa.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login con correo y contraseña", description = "Autentica un usuario con credenciales propias y retorna un token JWT.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @Operation(summary = "Registro de paciente", description = "Crea una cuenta nueva para un paciente. Valida documento, teléfono y edad según el tipo de documento.")
    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<AuthResponse>> registro(@Valid @RequestBody RegistroRequest request) {
        AuthResponse response = authService.registro(request);
        return ResponseEntity.ok(ApiResponse.ok("Registro exitoso", response));
    }

    @Operation(summary = "Login con cuenta Microsoft", description = "Autentica un usuario con su cuenta institucional de Microsoft. Recibe el accessToken generado por MSAL.")
    @PostMapping("/microsoft")
    public ResponseEntity<ApiResponse<AuthResponse>> loginMicrosoft(
            @Valid @RequestBody MicrosoftLoginRequest request) {
        AuthResponse response = authService.loginMicrosoft(request);
        return ResponseEntity.ok(ApiResponse.ok("Login institucional exitoso", response));
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca el token JWT actual: cualquier petición posterior con este mismo token será rechazada, aunque no haya expirado.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada", null));
    }
}
