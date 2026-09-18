package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.AbrirAtencionRequest;
import com.neovision.clinico.historia.dto.AdendaRequest;
import com.neovision.clinico.historia.dto.AdendaResponse;
import com.neovision.clinico.historia.dto.AtencionResponse;
import com.neovision.clinico.historia.dto.AtencionResumenResponse;
import com.neovision.clinico.historia.dto.DevolverAtencionRequest;
import com.neovision.clinico.historia.dto.EnviarRevisionRequest;
import com.neovision.clinico.historia.dto.FinalizarAtencionRequest;
import com.neovision.clinico.historia.dto.GuardarAtencionRequest;
import com.neovision.clinico.historia.dto.VerificacionIntegridadResponse;
import com.neovision.clinico.historia.service.AtencionService;
import com.neovision.clinico.historia.service.HistoriaPdfService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Workflow de atenciones (encuentros) de historia clínica: apertura, edición de
 * borrador, envío a revisión, devolución, finalización con co-firma, adendas y
 * consulta. La autorización fina se aplica en {@link AtencionService}.
 */
@Tag(name = "Historia Clínica - Atenciones", description = "Diligenciamiento, revisión, firma y adendas de atenciones clínicas.")
@RestController
@RequestMapping("/api/historias/atenciones")
public class AtencionController {

    private final AtencionService atencionService;
    private final HistoriaPdfService pdfService;

    public AtencionController(AtencionService atencionService, HistoriaPdfService pdfService) {
        this.atencionService = atencionService;
        this.pdfService = pdfService;
    }

    @Operation(summary = "Abrir atención", description = "Crea una atención en BORRADOR a partir de una cita del estudiante.")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    @PostMapping
    public ResponseEntity<ApiResponse<AtencionResponse>> abrir(
            @Valid @RequestBody AbrirAtencionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Atención abierta",
                atencionService.abrirAtencion(req.getIdCita(), auth)));
    }

    @Operation(summary = "Detalle de atención", description = "Retorna todos los folios + adendas. Registra auditoría de consulta.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AtencionResponse>> detalle(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Atención", atencionService.obtenerDetalle(id, auth)));
    }

    @Operation(summary = "Verificar integridad", description = "Recalcula el sello SHA-256 y lo compara con el almacenado. Indica si la atención fue alterada.")
    @GetMapping("/{id}/integridad")
    public ResponseEntity<ApiResponse<VerificacionIntegridadResponse>> integridad(
            @PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Verificación de integridad",
                atencionService.verificarIntegridad(id, auth)));
    }

    @Operation(summary = "Exportar atención a PDF", description = "Genera el PDF del Examen Funcional Optométrico. Solo docente y admin (gobernanza de datos clínicos).")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCENTE','ROLE_ADMIN')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable Integer id, Authentication auth) {
        byte[] pdf = pdfService.generar(id, auth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historia-clinica-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Guardar borrador", description = "Actualiza los campos clínicos. Solo el estudiante dueño y si la atención es editable.")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AtencionResponse>> guardar(
            @PathVariable Integer id, @RequestBody GuardarAtencionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Borrador guardado",
                atencionService.guardarBorrador(id, req, auth)));
    }

    @Operation(summary = "Enviar a revisión", description = "Pasa la atención a PENDIENTE_REVISION para el docente.")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    @PostMapping("/{id}/enviar-revision")
    public ResponseEntity<ApiResponse<AtencionResponse>> enviarRevision(
            @PathVariable Integer id, @RequestBody(required = false) EnviarRevisionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Enviada a revisión",
                atencionService.enviarRevision(id, req, auth)));
    }

    @Operation(summary = "Devolver atención", description = "El docente la devuelve al estudiante con un motivo.")
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    @PostMapping("/{id}/devolver")
    public ResponseEntity<ApiResponse<AtencionResponse>> devolver(
            @PathVariable Integer id, @Valid @RequestBody DevolverAtencionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Atención devuelta", atencionService.devolver(id, req, auth)));
    }

    @Operation(summary = "Finalizar (firmar)", description = "El docente firma y finaliza; la atención queda inmutable con sello de integridad.")
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ApiResponse<AtencionResponse>> finalizar(
            @PathVariable Integer id, @RequestBody(required = false) FinalizarAtencionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Atención finalizada", atencionService.finalizar(id, req, auth)));
    }

    @Operation(summary = "Agregar adenda", description = "Corrección append-only sobre una atención finalizada. Estudiante o docente.")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE','ROLE_DOCENTE')")
    @PostMapping("/{id}/adendas")
    public ResponseEntity<ApiResponse<AdendaResponse>> agregarAdenda(
            @PathVariable Integer id, @Valid @RequestBody AdendaRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Adenda agregada", atencionService.agregarAdenda(id, req, auth)));
    }

    @Operation(summary = "Mis atenciones", description = "Atenciones del estudiante autenticado.")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    @GetMapping("/mias")
    public ResponseEntity<ApiResponse<List<AtencionResumenResponse>>> mias(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Mis atenciones", atencionService.misAtenciones(auth)));
    }

    @Operation(summary = "Bandeja de revisión", description = "Atenciones PENDIENTE_REVISION de las sesiones del docente.")
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    @GetMapping("/bandeja")
    public ResponseEntity<ApiResponse<List<AtencionResumenResponse>>> bandeja(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Bandeja de revisión", atencionService.bandejaRevision(auth)));
    }
}
