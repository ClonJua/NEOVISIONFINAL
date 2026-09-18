package com.neovision.clinico.controller;

import com.neovision.clinico.dto.EspacioRequest;
import com.neovision.clinico.dto.EspacioResponse;
import com.neovision.clinico.dto.SalonRequest;
import com.neovision.clinico.dto.SalonResponse;
import com.neovision.clinico.service.EspacioService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Espacios y Salones", description = "Gestión de espacios físicos y sus salones de atención. Solo accesible por administradores.")
@RestController
@RequestMapping("/api/admin/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    // --- Espacios ---

    @Operation(summary = "Crear espacio", description = "Crea un nuevo espacio físico (ej: Clínica de Optometría).")
    @PostMapping
    public ResponseEntity<ApiResponse<EspacioResponse>> crear(@Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Espacio creado", espacioService.crearEspacio(request)));
    }

    @Operation(summary = "Listar espacios", description = "Retorna todos los espacios con sus salones.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EspacioResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Espacios listados", espacioService.listarEspacios()));
    }

    @Operation(summary = "Editar espacio", description = "Actualiza el nombre de un espacio existente.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EspacioResponse>> editar(
            @PathVariable Integer id, @Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Espacio actualizado", espacioService.editarEspacio(id, request)));
    }

    @Operation(summary = "Desactivar espacio", description = "Desactiva un espacio sin eliminarlo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        espacioService.desactivarEspacio(id);
        return ResponseEntity.ok(ApiResponse.ok("Espacio desactivado", null));
    }

    @Operation(summary = "Activar espacio", description = "Reactiva un espacio previamente desactivado.")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activarEspacio(@PathVariable Integer id) {
        espacioService.activarEspacio(id);
        return ResponseEntity.ok(ApiResponse.ok("Espacio activado", null));
    }

    @Operation(summary = "Eliminar espacio", description = "Elimina un espacio permanentemente de la base de datos.")
    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminarEspacio(@PathVariable Integer id) {
        espacioService.eliminarEspacio(id);
        return ResponseEntity.ok(ApiResponse.ok("Espacio eliminado permanentemente", null));
    }

    // --- Salones ---

    @Operation(summary = "Crear salón", description = "Crea un salón dentro de un espacio existente.")
    @PostMapping("/salones")
    public ResponseEntity<ApiResponse<SalonResponse>> crearSalon(@Valid @RequestBody SalonRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Salon creado", espacioService.crearSalon(request)));
    }

    @Operation(summary = "Editar salón", description = "Actualiza los datos de un salón existente.")
    @PutMapping("/salones/{id}")
    public ResponseEntity<ApiResponse<SalonResponse>> editarSalon(
            @PathVariable Integer id, @Valid @RequestBody SalonRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Salon actualizado", espacioService.editarSalon(id, request)));
    }

    @Operation(summary = "Listar salones por espacio", description = "Retorna todos los salones que pertenecen a un espacio específico.")
    @GetMapping("/{idEspacio}/salones")
    public ResponseEntity<ApiResponse<List<SalonResponse>>> listarSalones(@PathVariable Integer idEspacio) {
        return ResponseEntity.ok(ApiResponse.ok("Salones listados", espacioService.listarSalonesPorEspacio(idEspacio)));
    }

    @Operation(summary = "Desactivar salón", description = "Desactiva un salón sin eliminarlo.")
    @DeleteMapping("/salones/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivarSalon(@PathVariable Integer id) {
        espacioService.desactivarSalon(id);
        return ResponseEntity.ok(ApiResponse.ok("Salon desactivado", null));
    }

    @Operation(summary = "Activar salón", description = "Reactiva un salón previamente desactivado.")
    @PatchMapping("/salones/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activarSalon(@PathVariable Integer id) {
        espacioService.activarSalon(id);
        return ResponseEntity.ok(ApiResponse.ok("Salon activado", null));
    }

    @Operation(summary = "Eliminar salón", description = "Elimina un salón permanentemente de la base de datos.")
    @DeleteMapping("/salones/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminarSalon(@PathVariable Integer id) {
        espacioService.eliminarSalon(id);
        return ResponseEntity.ok(ApiResponse.ok("Salon eliminado permanentemente", null));
    }
}
