package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.AbrirHistoriaRequest;
import com.neovision.clinico.historia.dto.AuditoriaResponse;
import com.neovision.clinico.historia.dto.HistoriaClinicaResponse;
import com.neovision.clinico.historia.dto.VerificacionCadenaResponse;
import com.neovision.clinico.historia.service.AuditoriaService;
import com.neovision.clinico.historia.service.HistoriaClinicaService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Historia clínica: apertura, consulta por paciente, listado (admin) y auditoría.
 * El acceso de path (estudiante/docente/admin) lo controla SecurityConfig; las
 * reglas finas viven en el servicio.
 */
@Tag(name = "Historia Clínica", description = "Apertura, consulta y auditoría de historias clínicas.")
@RestController
@RequestMapping("/api/historias")
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaService;
    private final AuditoriaService auditoriaService;

    public HistoriaClinicaController(HistoriaClinicaService historiaService, AuditoriaService auditoriaService) {
        this.historiaService = historiaService;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Historia de un paciente", description = "Retorna la historia y el resumen de atenciones. Si idHistoria es null, el paciente aún no tiene historia abierta.")
    @GetMapping("/paciente/{idPaciente}")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> obtenerPorPaciente(
            @PathVariable Integer idPaciente, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Historia clínica",
                historiaService.obtenerPorPaciente(idPaciente, auth)));
    }

    @Operation(summary = "Abrir historia", description = "Crea la historia del paciente y registra el consentimiento informado. Solo estudiantes.")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    @PostMapping("/paciente/{idPaciente}/abrir")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> abrir(
            @PathVariable Integer idPaciente,
            @Valid @RequestBody AbrirHistoriaRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Historia abierta",
                historiaService.abrirHistoria(idPaciente, req, auth)));
    }

    @Operation(summary = "Listar historias (admin)", description = "Listado de historias para consulta de la secretaría/admin.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoriaClinicaResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok("Historias clínicas", historiaService.listarTodas()));
    }

    @Operation(summary = "Auditoría de un paciente", description = "Bitácora append-only de acciones sobre la historia. Solo docente y admin.")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCENTE','ROLE_ADMIN')")
    @GetMapping("/paciente/{idPaciente}/auditoria")
    public ResponseEntity<ApiResponse<List<AuditoriaResponse>>> auditoria(@PathVariable Integer idPaciente) {
        return ResponseEntity.ok(ApiResponse.ok("Auditoría", auditoriaService.consultarPorPaciente(idPaciente)));
    }

    @Operation(summary = "Verificar cadena de auditoría", description = "Recalcula el hash encadenado de toda la bitácora y confirma si fue alterada. Solo admin.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/auditoria/verificar-cadena")
    public ResponseEntity<ApiResponse<VerificacionCadenaResponse>> verificarCadenaAuditoria() {
        return ResponseEntity.ok(ApiResponse.ok("Verificación de cadena", auditoriaService.verificarCadena()));
    }
}
