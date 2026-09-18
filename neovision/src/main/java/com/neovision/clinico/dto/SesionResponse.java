package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "Datos de una sesión de práctica clínica.")
@Getter
@AllArgsConstructor
public class SesionResponse {
    @Schema(description = "ID único de la sesión.")
    private Integer idSesion;
    @Schema(description = "Fecha de la sesión (YYYY-MM-DD).")
    private String fecha;
    @Schema(description = "Hora de inicio (HH:mm:ss). Null si no se definió.")
    private String horaInicio;
    @Schema(description = "Hora de fin (HH:mm:ss). Null si no se definió.")
    private String horaFin;
    @Schema(description = "Número total de cupos de la sesión.")
    private Integer cuposTotales;
    @Schema(description = "Número de cupos aún disponibles para reservar.")
    private Integer cuposDisponibles;
    @Schema(description = "Indica si la sesión está activa.")
    private Boolean activo;
    @Schema(description = "ID del salón donde se realiza la sesión.")
    private Integer idSalon;
    @Schema(description = "Nombre del salón.")
    private String nombreSalon;
    @Schema(description = "Nombre del espacio clínico.")
    private String nombreEspacio;
    @Schema(description = "ID del docente responsable.")
    private Integer idDocente;
    @Schema(description = "Nombre completo del docente responsable.")
    private String nombreDocente;
    @Schema(description = "Lista de estudiantes asignados a la sesión.")
    private List<EstudianteInfo> estudiantes;

    @Schema(description = "Información básica de un estudiante en una sesión.")
    @Getter
    @AllArgsConstructor
    public static class EstudianteInfo {
        @Schema(description = "ID del estudiante.")
        private Integer idEstudiante;
        @Schema(description = "Nombre completo del estudiante.")
        private String nombre;
    }
}
