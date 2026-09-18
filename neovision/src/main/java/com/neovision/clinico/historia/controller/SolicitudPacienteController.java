package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.CrearSolicitudRequest;
import com.neovision.clinico.historia.dto.SolicitudResponse;
import com.neovision.clinico.historia.service.SolicitudHistoriaService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solicitudes de copia de historia clínica del paciente (derecho de acceso). */
@Tag(name = "Paciente - Solicitudes HC", description = "El paciente solicita una copia de su historia clínica.")
@RestController
@RequestMapping("/api/paciente/solicitudes")
public class SolicitudPacienteController {

    private final SolicitudHistoriaService service;

    public SolicitudPacienteController(SolicitudHistoriaService service) {
        this.service = service;
    }

    @Operation(summary = "Solicitar copia", description = "Crea una solicitud de copia de la historia clínica del paciente autenticado.")
    @PostMapping
    public ResponseEntity<ApiResponse<SolicitudResponse>> crear(
            @RequestBody(required = false) CrearSolicitudRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud creada", service.crear(req, auth)));
    }

    @Operation(summary = "Mis solicitudes", description = "Lista las solicitudes del paciente autenticado.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SolicitudResponse>>> mias(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Mis solicitudes", service.misSolicitudes(auth)));
    }
}
