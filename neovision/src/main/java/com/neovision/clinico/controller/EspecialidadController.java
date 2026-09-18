package com.neovision.clinico.controller;

import com.neovision.clinico.dto.EspecialidadRequest;
import com.neovision.clinico.dto.EspecialidadResponse;
import com.neovision.clinico.service.EspecialidadService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Especialidades", description = "Gestión del catálogo de especialidades clínicas.")
@RestController
@RequestMapping("/api")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @Operation(summary = "Listar especialidades activas", description = "Lectura abierta a usuarios autenticados (paciente/estudiante/docente/admin).")
    @GetMapping("/especialidades")
    public ResponseEntity<ApiResponse<List<EspecialidadResponse>>> listarActivas() {
        return ResponseEntity.ok(ApiResponse.ok("Especialidades activas", especialidadService.listarActivas()));
    }

    @Operation(summary = "Listar todas las especialidades (admin)", description = "Incluye desactivadas; sólo administradores.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/especialidades")
    public ResponseEntity<ApiResponse<List<EspecialidadResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok("Todas las especialidades", especialidadService.listarTodas()));
    }

    @Operation(summary = "Crear especialidad", description = "Crea una especialidad nueva en el catálogo.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/especialidades")
    public ResponseEntity<ApiResponse<EspecialidadResponse>> crear(@Valid @RequestBody EspecialidadRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Especialidad creada", especialidadService.crear(req)));
    }

    @Operation(summary = "Actualizar especialidad")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/especialidades/{id}")
    public ResponseEntity<ApiResponse<EspecialidadResponse>> actualizar(@PathVariable Integer id,
                                                                        @Valid @RequestBody EspecialidadRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Especialidad actualizada", especialidadService.actualizar(id, req)));
    }

    @Operation(summary = "Desactivar especialidad")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/especialidades/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        especialidadService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Especialidad desactivada", null));
    }

    @Operation(summary = "Reactivar especialidad")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/especialidades/{id}/reactivar")
    public ResponseEntity<ApiResponse<Void>> reactivar(@PathVariable Integer id) {
        especialidadService.reactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Especialidad reactivada", null));
    }
}
