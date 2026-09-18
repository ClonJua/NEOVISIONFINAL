package com.neovision.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CitaResponse {
    private Integer idCita;
    private String estado;
    private String horaInicio;
    private String horaFin;
    private String observaciones;
    private String fechaReserva;
    // Sesion
    private Integer idSesion;
    private String fechaSesion;
    private String nombreSalon;
    private String nombreEspacio;
    private String nombreDocente;
    // Estudiantes (el segundo es opcional: los pacientes se atienden normalmente en pareja)
    private Integer idEstudiante;
    private String nombreEstudiante;
    private Integer idEstudiante2;
    private String nombreEstudiante2;
    // Paciente (null si DISPONIBLE)
    private Integer idPaciente;
    private String nombrePaciente;
    private String apellidoPaciente;
    private String tipoDocumentoPaciente;
    private String numeroDocumentoPaciente;
    // Especialidad de la cita (null si aún no reservada)
    private Integer idEspecialidad;
    private String nombreEspecialidad;
    private String observacionRecordatorio;
    private String subtipoControl;
    // Especialidades del docente que supervisa la sesion (para que el paciente sepa qué puede pedir)
    private java.util.List<EspecialidadResponse> especialidadesDocente;
}
