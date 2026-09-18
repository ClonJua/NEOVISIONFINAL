package com.neovision.clinico.controller;

import com.neovision.clinico.dto.CambiarEstadoCitaRequest;
import com.neovision.clinico.dto.CitaRequest;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.DistribucionEstudianteResponse;
import com.neovision.clinico.dto.EliminarVencidasResponse;
import com.neovision.clinico.dto.EnviarExportRequest;
import com.neovision.clinico.dto.IndicadoresResponse;
import com.neovision.clinico.service.CitaService;
import com.neovision.clinico.service.ExportService;
import com.neovision.common.dto.ApiResponse;
import com.neovision.common.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/citas")
public class CitaAdminController {

    private final CitaService citaService;
    private final ExportService exportService;
    private final EmailService emailService;

    public CitaAdminController(CitaService citaService, ExportService exportService, EmailService emailService) {
        this.citaService = citaService;
        this.exportService = exportService;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CitaResponse>> crearBloque(
            @Valid @RequestBody CitaRequest request) {
        CitaResponse response = citaService.crearBloque(request);
        String msg = request.getIdPaciente() != null
                ? "Bloque creado y paciente asignado"
                : "Bloque disponible creado";
        return ResponseEntity.ok(ApiResponse.ok(msg, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CitaResponse>>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ApiResponse.ok("Citas listadas", citaService.listarCitas(estado)));
    }

    /**
     * Listado paginado. Escalable para volumenes grandes.
     * Uso: GET /api/admin/citas/paginadas?page=0&size=20&sort=idCita,desc&estado=RESERVADO
     */
    @GetMapping("/paginadas")
    public ResponseEntity<ApiResponse<Page<CitaResponse>>> listarPaginadas(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = "idCita", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Citas paginadas", citaService.listarCitas(estado, pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CitaResponse>> editar(
            @PathVariable Integer id, @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cita actualizada", citaService.editarCita(id, request)));
    }

    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        citaService.eliminarCita(id);
        return ResponseEntity.ok(ApiResponse.ok("Cita eliminada permanentemente", null));
    }

    // Limpieza en lote de bloques VENCIDA; omite los que ya tienen atención/historia clínica asociada.
    @DeleteMapping("/vencidas")
    public ResponseEntity<ApiResponse<EliminarVencidasResponse>> eliminarVencidas() {
        EliminarVencidasResponse resultado = citaService.eliminarVencidas();
        String mensaje = "Se eliminaron " + resultado.getEliminadas() + " cita(s) vencida(s)"
                + (resultado.getOmitidas() > 0 ? "; " + resultado.getOmitidas() + " se omitieron por tener historia clínica asociada" : "");
        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<CitaResponse>> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoCitaRequest request) {
        CitaResponse response = citaService.cambiarEstado(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado", response));
    }

    @PatchMapping("/{id}/reprogramar")
    public ResponseEntity<ApiResponse<CitaResponse>> reprogramar(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer idCitaNueva = body.get("idCitaNueva");
        if (idCitaNueva == null) {
            throw new RuntimeException("Debes indicar la cita nueva");
        }
        CitaResponse response = citaService.reprogramarAdmin(id, idCitaNueva);
        return ResponseEntity.ok(ApiResponse.ok("Cita reprogramada", response));
    }

    @GetMapping("/paciente/{idPaciente}")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> historialPaciente(
            @PathVariable Integer idPaciente) {
        return ResponseEntity.ok(ApiResponse.ok("Historial cargado", citaService.historialPaciente(idPaciente)));
    }

    // ===================== REPORTES =====================
    // Cada endpoint trae datos ya agregados o acotados a una ventana relevante,
    // en vez de que el frontend cargue todas las citas del sistema para calcular
    // los reportes en el navegador.

    @GetMapping("/reportes/indicadores")
    public ResponseEntity<ApiResponse<IndicadoresResponse>> reporteIndicadores() {
        return ResponseEntity.ok(ApiResponse.ok("Indicadores", citaService.indicadores()));
    }

    @GetMapping("/reportes/distribucion")
    public ResponseEntity<ApiResponse<List<DistribucionEstudianteResponse>>> reporteDistribucion() {
        return ResponseEntity.ok(ApiResponse.ok("Distribucion por estudiante", citaService.distribucionPorEstudiante()));
    }

    @GetMapping("/reportes/ocupacion")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> reporteOcupacion() {
        return ResponseEntity.ok(ApiResponse.ok("Ocupacion reciente", citaService.ocupacionReciente()));
    }

    @GetMapping("/reportes/inasistencias")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> reporteInasistencias() {
        return ResponseEntity.ok(ApiResponse.ok("Inasistencias", citaService.inasistencias()));
    }

    // ===================== METAS DE PACIENTES POR SEMESTRE =====================

    @GetMapping("/reportes/metas-semestre")
    public ResponseEntity<ApiResponse<List<com.neovision.clinico.dto.MetaSemestreResponse>>> listarMetasSemestre() {
        return ResponseEntity.ok(ApiResponse.ok("Metas por semestre", citaService.listarMetasSemestre()));
    }

    @PutMapping("/reportes/metas-semestre/{semestre}")
    public ResponseEntity<ApiResponse<com.neovision.clinico.dto.MetaSemestreResponse>> actualizarMetaSemestre(
            @PathVariable Integer semestre,
            @RequestBody Map<String, Integer> body) {
        Integer minimoPacientes = body.get("minimoPacientes");
        if (minimoPacientes == null || minimoPacientes < 1) {
            throw new RuntimeException("El mínimo de pacientes es obligatorio y debe ser mayor a 0");
        }
        var response = citaService.actualizarMetaSemestre(semestre, minimoPacientes);
        return ResponseEntity.ok(ApiResponse.ok("Meta actualizada", response));
    }

    // ===================== AVANCE DE SEMESTRE EN LOTE =====================

    @GetMapping("/reportes/avance-semestre")
    public ResponseEntity<ApiResponse<List<com.neovision.clinico.dto.CandidatoAvanceSemestreResponse>>> listarCandidatosAvanceSemestre() {
        return ResponseEntity.ok(ApiResponse.ok("Candidatos a avance de semestre", citaService.listarCandidatosAvanceSemestre()));
    }

    @PostMapping("/reportes/avance-semestre")
    public ResponseEntity<ApiResponse<Integer>> avanzarSemestre(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("idsEstudiantes");
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Debes seleccionar al menos un estudiante");
        }
        int avanzados = citaService.avanzarSemestre(ids);
        return ResponseEntity.ok(ApiResponse.ok(avanzados + " estudiante(s) avanzados de semestre", avanzados));
    }

    /**
     * GET /api/admin/citas/exportar?formato=csv|excel|pdf&estado=RESERVADO (opcional)
     * Devuelve el archivo binario listo para descarga.
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam String formato,
            @RequestParam(required = false) String estado) {

        return switch (formato.toLowerCase()) {
            case "csv" -> {
                byte[] data = exportService.exportarCSV(estado);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=citas.csv")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(data);
            }
            case "excel" -> {
                byte[] data = exportService.exportarExcel(estado);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=citas.xlsx")
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(data);
            }
            case "pdf" -> {
                byte[] data = exportService.exportarPDF(estado);
                yield ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=citas.pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(data);
            }
            default -> ResponseEntity.badRequest().<byte[]>body(null);
        };
    }

    /**
     * POST /api/admin/citas/exportar/enviar
     * Genera el mismo reporte de {@link #exportar} pero lo envía por correo en vez de descargarlo.
     */
    @PostMapping("/exportar/enviar")
    public ResponseEntity<ApiResponse<Void>> exportarPorCorreo(@Valid @RequestBody EnviarExportRequest request) {
        byte[] data;
        String nombreArchivo;
        String contentType;
        switch (request.getFormato().toLowerCase()) {
            case "csv" -> {
                data = exportService.exportarCSV(request.getEstado());
                nombreArchivo = "citas.csv";
                contentType = "text/csv; charset=UTF-8";
            }
            case "excel" -> {
                data = exportService.exportarExcel(request.getEstado());
                nombreArchivo = "citas.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case "pdf" -> {
                data = exportService.exportarPDF(request.getEstado());
                nombreArchivo = "citas.pdf";
                contentType = "application/pdf";
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Formato de exportacion invalido"));
            }
        }
        emailService.enviarReporteAdjunto(request.getCorreoDestino(),
                "NeoVision - Reporte de citas",
                "Adjuntamos el reporte de citas exportado desde NeoVision.",
                nombreArchivo, data, contentType);
        return ResponseEntity.ok(ApiResponse.ok("Reporte enviado a " + request.getCorreoDestino(), null));
    }
}
