package com.neovision.clinico.controller;

import com.neovision.admin.dto.EditarPacienteRequest;
import com.neovision.admin.dto.PacienteResponse;
import com.neovision.admin.service.AdminService;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.ReservarBloqueRequest;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.clinico.service.CitaService;
import com.neovision.common.dto.ApiResponse;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del paciente: ver bloques disponibles, reservar, cancelar citas y gestionar su perfil.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Paciente - Citas", description = "Operaciones del paciente: ver disponibilidad, reservar, cancelar y perfil.")
@RestController
@RequestMapping("/api/paciente")
public class CitaPacienteController {

    private final CitaService citaService;
    private final AdminService adminService;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public CitaPacienteController(CitaService citaService, AdminService adminService,
                                  PacienteRepository pacienteRepository,
                                  UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.adminService = adminService;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Retorna los bloques de cita disponibles para reservar.
     *  Si se provee idEspecialidad, filtra por docentes que la atienden (o cualquiera si es default). */
    @io.swagger.v3.oas.annotations.Operation(summary = "Bloques disponibles", description = "Lista los bloques en DISPONIBLE; opcionalmente filtra por especialidad.")
    @GetMapping("/citas/disponibles")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> bloquesDisponibles(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer idEspecialidad) {
        List<CitaResponse> data = (idEspecialidad == null)
                ? citaService.bloquesDisponibles()
                : citaService.bloquesDisponiblesPorEspecialidad(idEspecialidad);
        return ResponseEntity.ok(ApiResponse.ok("Bloques disponibles", data));
    }

    /** Reserva un bloque disponible para el paciente autenticado, indicando la especialidad. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Reservar bloque", description = "El paciente reserva un bloque y elige especialidad (Funcional por defecto). Cambia el estado a RESERVADO.")
    @PostMapping("/citas/{idCita}/reservar")
    public ResponseEntity<ApiResponse<CitaResponse>> reservar(
            Authentication auth,
            @PathVariable Integer idCita,
            @RequestBody(required = false) ReservarBloqueRequest body) {
        Integer idEsp = body != null ? body.getIdEspecialidad() : null;
        String subtipo = body != null ? body.getSubtipoControl() : null;
        CitaResponse response = citaService.reservarBloque(auth.getName(), idCita, idEsp, subtipo);
        return ResponseEntity.ok(ApiResponse.ok("Bloque reservado exitosamente", response));
    }

    /** Cancela una cita reservada del paciente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Cancelar cita", description = "Cancela una cita RESERVADO del paciente. Acepta observaciones opcionales en el body.")
    @PatchMapping("/citas/{idCita}/cancelar")
    public ResponseEntity<ApiResponse<CitaResponse>> cancelar(
            Authentication auth,
            @PathVariable Integer idCita,
            @RequestBody(required = false) Map<String, String> body) {
        String observaciones = body != null ? body.get("observaciones") : null;
        CitaResponse response = citaService.cancelarPaciente(auth.getName(), idCita, observaciones);
        return ResponseEntity.ok(ApiResponse.ok("Cita cancelada", response));
    }

    /** Retorna el historial de citas del paciente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Mis citas", description = "Retorna todas las citas del paciente autenticado, en cualquier estado.")
    @GetMapping("/citas")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> misCitas(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Mis citas", citaService.misCitas(auth.getName())));
    }

    /** Retorna el perfil del paciente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Mi perfil", description = "Retorna los datos del perfil del paciente actualmente autenticado.")
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<PacienteResponse>> miPerfil(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Paciente paciente = pacienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        PacienteResponse response = adminService.obtenerPaciente(paciente.getIdPaciente());
        return ResponseEntity.ok(ApiResponse.ok("Mi perfil", response));
    }

    /** Actualiza los datos del perfil del paciente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Editar perfil", description = "Actualiza los datos básicos del perfil del paciente autenticado.")
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<PacienteResponse>> editarPerfil(
            Authentication auth,
            @Valid @RequestBody EditarPacienteRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Paciente paciente = pacienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        PacienteResponse response = adminService.editarPaciente(paciente.getIdPaciente(), request);
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", response));
    }
}
