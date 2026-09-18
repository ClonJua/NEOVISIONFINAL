package com.neovision.admin.controller;

import com.neovision.admin.dto.CargaMasivaResponse;
import com.neovision.admin.dto.CrearDocenteRequest;
import com.neovision.admin.dto.CrearEstudianteRequest;
import com.neovision.admin.dto.CrearPacienteRequest;
import com.neovision.admin.dto.DocenteResponse;
import com.neovision.admin.dto.EditarDocenteAdminRequest;
import com.neovision.admin.dto.EditarEstudianteAdminRequest;
import com.neovision.admin.dto.EditarPacienteAdminRequest;
import com.neovision.admin.dto.EstudianteResponse;
import com.neovision.admin.dto.PacienteResponse;
import com.neovision.admin.service.AdminService;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin - Usuarios", description = "Gestión de docentes(endpoint), estudiantes y pacientes. Solo accesible por administradores.")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // --- Docentes ---

    @Operation(summary = "Crear docente", description = "Registra un nuevo docente en el sistema.")
    @PostMapping("/docentes")
    public ResponseEntity<ApiResponse<DocenteResponse>> crearDocente(
            @Valid @RequestBody CrearDocenteRequest request) {
        DocenteResponse response = adminService.crearDocente(request);
        return ResponseEntity.ok(ApiResponse.ok("Docente creado exitosamente", response));
    }

    @Operation(summary = "Listar docentes", description = "Retorna todos los docentes registrados.")
    @GetMapping("/docentes")
    public ResponseEntity<ApiResponse<List<DocenteResponse>>> listarDocentes() {
        return ResponseEntity.ok(ApiResponse.ok("Docentes listados", adminService.listarDocentes()));
    }

    @Operation(summary = "Obtener docente", description = "Retorna un docente por su ID.")
    @GetMapping("/docentes/{id}")
    public ResponseEntity<ApiResponse<DocenteResponse>> obtenerDocente(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Docente encontrado", adminService.obtenerDocente(id)));
    }

    @Operation(summary = "Editar docente", description = "Actualiza los datos de un docente existente.")
    @PutMapping("/docentes/{id}")
    public ResponseEntity<ApiResponse<DocenteResponse>> editarDocente(
            @PathVariable Integer id, @Valid @RequestBody EditarDocenteAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Docente actualizado", adminService.editarDocente(id, request)));
    }

    @Operation(summary = "Desactivar docente", description = "Desactiva un docente sin eliminarlo de la base de datos.")
    @DeleteMapping("/docentes/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivarDocente(@PathVariable Integer id) {
        adminService.desactivarDocente(id);
        return ResponseEntity.ok(ApiResponse.ok("Docente desactivado", null));
    }

    @Operation(summary = "Reactivar docente", description = "Reactiva un docente previamente desactivado.")
    @PatchMapping("/docentes/{id}/reactivar")
    public ResponseEntity<ApiResponse<Void>> reactivarDocente(@PathVariable Integer id) {
        adminService.reactivarDocente(id);
        return ResponseEntity.ok(ApiResponse.ok("Docente reactivado", null));
    }

    @Operation(summary = "Eliminar docente", description = "Elimina un docente permanentemente de la base de datos.")
    @DeleteMapping("/docentes/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminarDocente(@PathVariable Integer id) {
        adminService.eliminarDocente(id);
        return ResponseEntity.ok(ApiResponse.ok("Docente eliminado permanentemente", null));
    }

    // --- Estudiantes ---

    @Operation(summary = "Crear estudiante", description = "Registra un nuevo estudiante en el sistema.")
    @PostMapping("/estudiantes")
    public ResponseEntity<ApiResponse<EstudianteResponse>> crearEstudiante(
            @Valid @RequestBody CrearEstudianteRequest request) {
        EstudianteResponse response = adminService.crearEstudiante(request);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante creado exitosamente", response));
    }

    @Operation(summary = "Listar estudiantes", description = "Retorna todos los estudiantes registrados.")
    @GetMapping("/estudiantes")
    public ResponseEntity<ApiResponse<List<EstudianteResponse>>> listarEstudiantes() {
        return ResponseEntity.ok(ApiResponse.ok("Estudiantes listados", adminService.listarEstudiantes()));
    }

    @Operation(summary = "Obtener estudiante", description = "Retorna un estudiante por su ID.")
    @GetMapping("/estudiantes/{id}")
    public ResponseEntity<ApiResponse<EstudianteResponse>> obtenerEstudiante(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante encontrado", adminService.obtenerEstudiante(id)));
    }

    @Operation(summary = "Editar estudiante", description = "Actualiza los datos de un estudiante existente.")
    @PutMapping("/estudiantes/{id}")
    public ResponseEntity<ApiResponse<EstudianteResponse>> editarEstudiante(
            @PathVariable Integer id, @Valid @RequestBody EditarEstudianteAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante actualizado", adminService.editarEstudiante(id, request)));
    }

    @Operation(summary = "Desactivar estudiante", description = "Desactiva un estudiante sin eliminarlo de la base de datos.")
    @DeleteMapping("/estudiantes/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivarEstudiante(@PathVariable Integer id) {
        adminService.desactivarEstudiante(id);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante desactivado", null));
    }

    @Operation(summary = "Reactivar estudiante", description = "Reactiva un estudiante previamente desactivado.")
    @PatchMapping("/estudiantes/{id}/reactivar")
    public ResponseEntity<ApiResponse<Void>> reactivarEstudiante(@PathVariable Integer id) {
        adminService.reactivarEstudiante(id);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante reactivado", null));
    }

    @Operation(summary = "Eliminar estudiante", description = "Elimina un estudiante permanentemente de la base de datos.")
    @DeleteMapping("/estudiantes/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminarEstudiante(@PathVariable Integer id) {
        adminService.eliminarEstudiante(id);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante eliminado permanentemente", null));
    }

    // --- Carga masiva (Excel) ---

    @Operation(summary = "Carga masiva de docentes",
            description = "Crea varios docentes desde un Excel (.xlsx) con las columnas de la plantilla. Cada fila se procesa por separado: las que fallan se reportan y las demás sí se crean.")
    @PostMapping(value = "/docentes/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CargaMasivaResponse>> cargarDocentesDesdeExcel(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(ApiResponse.ok("Carga masiva procesada", adminService.cargarDocentesDesdeExcel(archivo)));
    }

    @Operation(summary = "Plantilla de carga masiva de docentes",
            description = "Excel con la cabecera esperada y una hoja de instrucciones con un ejemplo.")
    @GetMapping("/docentes/carga-masiva/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaDocentes() {
        return plantilla("plantilla_docentes.xlsx", adminService.descargarPlantillaDocentes());
    }

    @Operation(summary = "Carga masiva de estudiantes",
            description = "Crea varios estudiantes desde un Excel (.xlsx) con las columnas de la plantilla. Cada fila se procesa por separado: las que fallan se reportan y las demás sí se crean.")
    @PostMapping(value = "/estudiantes/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CargaMasivaResponse>> cargarEstudiantesDesdeExcel(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(ApiResponse.ok("Carga masiva procesada", adminService.cargarEstudiantesDesdeExcel(archivo)));
    }

    @Operation(summary = "Plantilla de carga masiva de estudiantes",
            description = "Excel con la cabecera esperada y una hoja de instrucciones con un ejemplo.")
    @GetMapping("/estudiantes/carga-masiva/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaEstudiantes() {
        return plantilla("plantilla_estudiantes.xlsx", adminService.descargarPlantillaEstudiantes());
    }

    private ResponseEntity<byte[]> plantilla(String nombreArchivo, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }

    // --- Pacientes ---

    @Operation(summary = "Crear paciente", description = "Registra un nuevo paciente en el sistema.")
    @PostMapping("/pacientes")
    public ResponseEntity<ApiResponse<PacienteResponse>> crearPaciente(
            @Valid @RequestBody CrearPacienteRequest request) {
        PacienteResponse response = adminService.crearPaciente(request);
        return ResponseEntity.ok(ApiResponse.ok("Paciente creado exitosamente", response));
    }

    @Operation(summary = "Listar pacientes", description = "Retorna todos los pacientes registrados.")
    @GetMapping("/pacientes")
    public ResponseEntity<ApiResponse<List<PacienteResponse>>> listarPacientes() {
        return ResponseEntity.ok(ApiResponse.ok("Pacientes listados", adminService.listarPacientes()));
    }

    @Operation(summary = "Editar paciente", description = "Actualiza los datos de un paciente existente.")
    @PutMapping("/pacientes/{id}")
    public ResponseEntity<ApiResponse<PacienteResponse>> editarPaciente(
            @PathVariable Integer id, @Valid @RequestBody EditarPacienteAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Paciente actualizado", adminService.editarPaciente(id, request)));
    }

    @Operation(summary = "Desactivar paciente", description = "Desactiva un paciente sin eliminarlo de la base de datos.")
    @DeleteMapping("/pacientes/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivarPaciente(@PathVariable Integer id) {
        adminService.desactivarPaciente(id);
        return ResponseEntity.ok(ApiResponse.ok("Paciente desactivado", null));
    }

    @Operation(summary = "Restablecer contraseña de paciente", description = "El admin/secretaría fija una nueva contraseña para el paciente (no hay flujo de recuperación por correo).")
    @PatchMapping("/pacientes/{id}/contrasena")
    public ResponseEntity<ApiResponse<Void>> resetearContrasenaPaciente(
            @PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        String nuevaContrasena = body.get("nuevaContrasena");
        if (nuevaContrasena == null || nuevaContrasena.isBlank()) {
            throw new RuntimeException("La nueva contraseña es obligatoria");
        }
        adminService.resetearContrasenaPaciente(id, nuevaContrasena);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña restablecida", null));
    }

    @Operation(summary = "Reactivar paciente", description = "Reactiva un paciente previamente desactivado.")
    @PatchMapping("/pacientes/{id}/reactivar")
    public ResponseEntity<ApiResponse<Void>> reactivarPaciente(@PathVariable Integer id) {
        adminService.reactivarPaciente(id);
        return ResponseEntity.ok(ApiResponse.ok("Paciente reactivado", null));
    }

    @Operation(summary = "Eliminar paciente", description = "Elimina un paciente permanentemente de la base de datos.")
    @DeleteMapping("/pacientes/{id}/permanente")
    public ResponseEntity<ApiResponse<Void>> eliminarPaciente(@PathVariable Integer id) {
        adminService.eliminarPaciente(id);
        return ResponseEntity.ok(ApiResponse.ok("Paciente eliminado permanentemente", null));
    }
}
