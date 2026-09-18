package com.neovision.clinico.controller;

import com.neovision.admin.dto.CrearPacienteRequest;
import com.neovision.admin.dto.EditarEstudianteRequest;
import com.neovision.admin.dto.EstudianteResponse;
import com.neovision.admin.dto.PacienteResponse;
import com.neovision.admin.service.AdminService;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.ProgresoResponse;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.repository.EstudianteRepository;
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

@RestController
@RequestMapping("/api/estudiante")
public class CitaEstudianteController {

    private final CitaService citaService;
    private final AdminService adminService;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;

    public CitaEstudianteController(CitaService citaService, AdminService adminService,
                                    EstudianteRepository estudianteRepository,
                                    UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.adminService = adminService;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/citas")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> misBloques(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Mis bloques", citaService.misBloques(auth.getName())));
    }

    @GetMapping("/citas/progreso")
    public ResponseEntity<ApiResponse<ProgresoResponse>> progreso(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Progreso", citaService.progresoEstudiante(auth.getName())));
    }

    @PostMapping("/citas/{idCita}/asignar-paciente")
    public ResponseEntity<ApiResponse<CitaResponse>> asignarPaciente(
            Authentication auth,
            @PathVariable Integer idCita,
            @RequestBody Map<String, Integer> body) {
        Integer idPaciente = body.get("idPaciente");
        if (idPaciente == null) {
            throw new RuntimeException("Debes indicar el paciente");
        }
        CitaResponse response = citaService.asignarPaciente(auth.getName(), idCita, idPaciente);
        return ResponseEntity.ok(ApiResponse.ok("Paciente asignado al bloque", response));
    }

    @PostMapping("/pacientes")
    public ResponseEntity<ApiResponse<PacienteResponse>> crearPaciente(
            @Valid @RequestBody CrearPacienteRequest request) {
        PacienteResponse response = adminService.crearPaciente(request);
        return ResponseEntity.ok(ApiResponse.ok("Paciente creado", response));
    }

    @GetMapping("/pacientes")
    public ResponseEntity<ApiResponse<List<PacienteResponse>>> listarPacientes() {
        return ResponseEntity.ok(ApiResponse.ok("Pacientes", adminService.listarPacientes()));
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<EstudianteResponse>> miPerfil(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        EstudianteResponse response = adminService.obtenerEstudiante(estudiante.getIdEstudiante());
        return ResponseEntity.ok(ApiResponse.ok("Mi perfil", response));
    }

    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<EstudianteResponse>> editarPerfil(
            Authentication auth,
            @Valid @RequestBody EditarEstudianteRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        EstudianteResponse response = adminService.editarEstudiante(estudiante.getIdEstudiante(), request);
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", response));
    }
}
