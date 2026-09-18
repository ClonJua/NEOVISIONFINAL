package com.neovision.usuario.controller;

import com.neovision.common.dto.ApiResponse;
import com.neovision.common.exception.UsuarioNoEncontradoException;
import com.neovision.usuario.dto.PushTokenRequest;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints comunes a cualquier usuario autenticado (paciente, estudiante, docente, admin).
 */
@Tag(name = "Usuario", description = "Operaciones del usuario autenticado: registro de dispositivo para notificaciones push.")
@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Registra/actualiza el Expo push token del dispositivo del usuario autenticado. */
    @Operation(summary = "Registrar push token", description = "Asocia el token de notificaciones push del dispositivo al usuario autenticado.")
    @PostMapping("/push-token")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> registrarPushToken(
            Authentication auth,
            @Valid @RequestBody PushTokenRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
        usuario.setExpoPushToken(request.getToken());
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Token registrado", null));
    }

    /** Elimina el push token del usuario (al cerrar sesion). */
    @Operation(summary = "Eliminar push token", description = "Desvincula el token de notificaciones push del usuario autenticado (logout).")
    @DeleteMapping("/push-token")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> eliminarPushToken(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
        usuario.setExpoPushToken(null);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Token eliminado", null));
    }

    /** Retorna la firma/sello registrada del usuario autenticado. */
    @Operation(summary = "Mi firma", description = "Retorna la firma y sello (PNG base64) del usuario autenticado, o null si no tiene.")
    @GetMapping("/firma")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<String>> obtenerFirma(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
        return ResponseEntity.ok(ApiResponse.ok("Firma", usuario.getFirma()));
    }

    /** Registra/actualiza la firma/sello del usuario autenticado. */
    @Operation(summary = "Guardar firma", description = "Guarda la firma y sello (PNG base64) del usuario autenticado para reutilizarla al firmar atenciones.")
    @PutMapping("/firma")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> guardarFirma(
            Authentication auth, @RequestBody FirmaRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
        usuario.setFirma(request.getFirma());
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Firma guardada", null));
    }

    /** Body para guardar la firma. */
    public static class FirmaRequest {
        private String firma;
        public String getFirma() { return firma; }
        public void setFirma(String firma) { this.firma = firma; }
    }
}
