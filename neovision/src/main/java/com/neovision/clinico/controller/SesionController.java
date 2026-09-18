package com.neovision.clinico.controller;

import com.neovision.clinico.dto.SesionRequest;
import com.neovision.clinico.dto.SesionResponse;
import com.neovision.clinico.dto.CrearSesionRecurrenteRequest;
import com.neovision.clinico.dto.SesionRecurrenteResponse;
import com.neovision.clinico.service.SesionService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Sesiones", description = "Gestión de sesiones prácticas donde se crean los bloques de atención. Solo accesible por administradores.")
@RestController
@RequestMapping("/api/admin/sesiones")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    @Operation(summary = "Crear sesión", description = "Crea una sesión práctica individual con fecha, hora y docente asignado.")
    @PostMapping
    public ResponseEntity<ApiResponse<SesionResponse>> crear(@Valid @RequestBody SesionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sesión creada", sesionService.crearSesion(request)));
    }

    @Operation(summary = "Crear sesiones recurrentes", description = "Crea múltiples sesiones automáticamente para un día de la semana en un rango de fechas. Omite festivos colombianos.")
    @PostMapping("/recurrente")
    public ResponseEntity<ApiResponse<SesionRecurrenteResponse>> crearRecurrente(
            @Valid @RequestBody CrearSesionRecurrenteRequest request) {
        SesionRecurrenteResponse response = sesionService.crearSesionesRecurrentes(request);
        return ResponseEntity.ok(ApiResponse.ok(response.getMensaje(), response));
    }

    @Operation(summary = "Listar sesiones", description = "Retorna todas las sesiones. Se puede filtrar por fecha con el parámetro ?fecha=YYYY-MM-DD.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SesionResponse>>> listar(
            @RequestParam(required = false) String fecha) {
        List<SesionResponse> sesiones = fecha != null
                ? sesionService.listarSesionesPorFecha(fecha)
                : sesionService.listarSesiones();
        return ResponseEntity.ok(ApiResponse.ok("Sesiones listadas", sesiones));
    }

    @Operation(summary = "Editar sesión", description = "Actualiza los datos de una sesión existente.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SesionResponse>> editar(
            @PathVariable Integer id, @Valid @RequestBody SesionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sesión actualizada", sesionService.editarSesion(id, request)));
    }

    @Operation(summary = "Desactivar sesión", description = "Desactiva una sesión para que no aparezca como disponible.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        sesionService.desactivarSesion(id);
        return ResponseEntity.ok(ApiResponse.ok("Sesión desactivada", null));
    }

    @Operation(summary = "Eliminar sesión", description = "Elimina una sesión permanentemente de la base de datos.")
    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        sesionService.eliminarSesion(id);
        return ResponseEntity.ok(ApiResponse.ok("Sesión eliminada permanentemente", null));
    }
}
