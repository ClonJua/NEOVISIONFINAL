package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.CrearSolicitudPresencialRequest;
import com.neovision.clinico.historia.dto.RechazarSolicitudRequest;
import com.neovision.clinico.historia.dto.SolicitudResponse;
import com.neovision.clinico.historia.service.SolicitudHistoriaService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestión (admin/secretaría) de las solicitudes de copia de historia clínica. */
@Tag(name = "Admin - Solicitudes HC", description = "El admin aprueba o rechaza solicitudes de copia de historia clínica.")
@RestController
@RequestMapping("/api/admin/solicitudes")
public class SolicitudAdminController {

    private final SolicitudHistoriaService service;

    public SolicitudAdminController(SolicitudHistoriaService service) {
        this.service = service;
    }

    @Operation(summary = "Listar solicitudes", description = "Todas las solicitudes de copia de historia clínica.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SolicitudResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Solicitudes", service.listarTodas()));
    }

    @Operation(summary = "Registrar solicitud presencial",
            description = "Para el paciente que pide su historia en la clínica y no tiene cuenta ni correo. "
                    + "Registra la solicitud a su nombre (dejando constancia de quién verificó su identidad) "
                    + "y devuelve el PDF para entregarlo en mano. Nace ya aprobada: el paciente está presente.")
    @PostMapping("/presencial")
    public ResponseEntity<byte[]> crearPresencial(
            @Valid @RequestBody CrearSolicitudPresencialRequest req, Authentication auth) {
        byte[] pdf = service.crearPresencial(req.getIdPaciente(), req.getMotivo(), auth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=historia-clinica-" + req.getIdPaciente() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Aprobar solicitud", description = "Genera el PDF de la historia y lo envía al correo registrado del paciente.")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ApiResponse<SolicitudResponse>> aprobar(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud aprobada y enviada al correo del paciente",
                service.aprobar(id, auth)));
    }

    @Operation(summary = "Rechazar solicitud", description = "Rechaza la solicitud con un motivo.")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ApiResponse<SolicitudResponse>> rechazar(
            @PathVariable Integer id, @Valid @RequestBody RechazarSolicitudRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud rechazada", service.rechazar(id, req.getMotivo(), auth)));
    }
}
