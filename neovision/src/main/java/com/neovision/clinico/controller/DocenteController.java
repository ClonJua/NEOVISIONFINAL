package com.neovision.clinico.controller;

import com.neovision.admin.dto.DocenteResponse;
import com.neovision.admin.dto.EditarDocenteRequest;
import com.neovision.admin.service.AdminService;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.SesionResponse;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.SesionPracticaRepository;
import com.neovision.clinico.service.CitaService;
import com.neovision.common.dto.ApiResponse;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del docente: ver sus sesiones asignadas, las citas de esas sesiones y su perfil.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Docente", description = "Operaciones del docente: ver sus sesiones, citas y gestionar su perfil.")
@RestController
@RequestMapping("/api/docente")
public class DocenteController {

    private final SesionPracticaRepository sesionRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminService adminService;
    private final CitaService citaService;

    public DocenteController(SesionPracticaRepository sesionRepository,
                             DocenteRepository docenteRepository,
                             UsuarioRepository usuarioRepository,
                             AdminService adminService,
                             CitaService citaService) {
        this.sesionRepository = sesionRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminService = adminService;
        this.citaService = citaService;
    }

    /** Retorna las sesiones activas asignadas al docente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Mis sesiones", description = "Retorna las sesiones de práctica activas donde el docente autenticado es el responsable.")
    @GetMapping("/sesiones")
    public ResponseEntity<ApiResponse<List<SesionResponse>>> misSesiones(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Docente docente = docenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        List<SesionResponse> sesiones = sesionRepository
                .findByDocenteWithDetails(docente.getIdDocente())
                .stream()
                .map(sesion -> {
                    var estudiantesInfo = sesion.getEstudiantes().stream()
                            .map(e -> new SesionResponse.EstudianteInfo(
                                    e.getIdEstudiante(), e.getNombre() + " " + e.getApellido()))
                            .toList();
                    var salon = sesion.getSalon();
                    return new SesionResponse(
                            sesion.getIdSesion(),
                            sesion.getFecha().toString(),
                            sesion.getHoraInicio() != null ? sesion.getHoraInicio().toString() : null,
                            sesion.getHoraFin() != null ? sesion.getHoraFin().toString() : null,
                            sesion.getCuposTotales(),
                            sesion.getCuposTotales(),
                            sesion.getActivo(),
                            salon != null ? salon.getIdSalon() : null,
                            salon != null ? salon.getNombre() : null,
                            salon != null ? salon.getEspacio().getNombre() : null,
                            sesion.getDocente().getIdDocente(),
                            sesion.getDocente().getNombre() + " " + sesion.getDocente().getApellido(),
                            estudiantesInfo
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Mis sesiones", sesiones));
    }

    /** Retorna el perfil del docente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Mi perfil", description = "Retorna los datos del perfil del docente actualmente autenticado.")
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<DocenteResponse>> miPerfil(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Docente docente = docenteRepository.findWithEspecialidadesByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        java.util.List<com.neovision.clinico.dto.EspecialidadResponse> especialidades =
                docente.getEspecialidades() == null ? java.util.Collections.emptyList()
                        : docente.getEspecialidades().stream()
                            .map(e -> new com.neovision.clinico.dto.EspecialidadResponse(
                                    e.getIdEspecialidad(), e.getNombre(),
                                    e.getObservacionRecordatorio(),
                                    e.getRequiereSubtipo(), e.getEsDefault(), e.getActivo()))
                            .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                            .toList();

        DocenteResponse response = new DocenteResponse(
                docente.getIdDocente(),
                docente.getNombre(),
                docente.getApellido(),
                docente.getCedula(),
                docente.getTelefono(),
                docente.getCorreo(),
                true,
                especialidades
        );

        return ResponseEntity.ok(ApiResponse.ok("Mi perfil", response));
    }

    /** Actualiza los datos del perfil del docente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Editar perfil", description = "Actualiza los datos básicos del perfil del docente autenticado.")
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<DocenteResponse>> editarPerfil(
            Authentication auth,
            @Valid @RequestBody EditarDocenteRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Docente docente = docenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        DocenteResponse response = adminService.editarDocente(docente.getIdDocente(), request);
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", response));
    }

    /** Retorna todas las citas de las sesiones del docente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Mis citas", description = "Retorna las citas de todas las sesiones donde el docente autenticado es el responsable.")
    @GetMapping("/citas")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> misCitas(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Docente docente = docenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        List<CitaResponse> citas = citaService.misCitasDocente(docente.getIdDocente());
        return ResponseEntity.ok(ApiResponse.ok("Mis citas", citas));
    }

    /** Confirma la asistencia (marca COMPLETADO) de una cita de una sesion asignada al docente autenticado. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Confirmar asistencia", description = "El docente responsable de la sesión confirma que el paciente asistió.")
    @PatchMapping("/citas/{idCita}/confirmar")
    public ResponseEntity<ApiResponse<CitaResponse>> confirmarAsistencia(
            Authentication auth,
            @PathVariable Integer idCita) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Docente docente = docenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        CitaResponse response = citaService.confirmarAsistenciaDocente(docente.getIdDocente(), idCita);
        return ResponseEntity.ok(ApiResponse.ok("Asistencia confirmada", response));
    }
}
