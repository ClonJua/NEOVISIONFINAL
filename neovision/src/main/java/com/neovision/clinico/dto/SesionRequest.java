package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "Datos para crear o actualizar una sesión de práctica clínica.")
@Getter
@Setter
public class SesionRequest {

    @Schema(description = "ID del salón donde se realiza la sesión. Opcional.", example = "3")
    private Integer idSalon;

    @Schema(description = "ID del docente responsable de la sesión.", example = "2")
    @NotNull
    private Integer idDocente;

    @Schema(description = "Fecha de la sesión en formato YYYY-MM-DD.", example = "2026-05-10")
    @NotBlank
    private String fecha;

    @Schema(description = "Hora de inicio en formato HH:mm. Opcional.", example = "08:00")
    private String horaInicio;

    @Schema(description = "Hora de fin en formato HH:mm. Opcional.", example = "12:00")
    private String horaFin;

    @Schema(description = "Número total de cupos (bloques) de la sesión.", example = "4")
    @NotNull
    @Min(1)
    private Integer cuposTotales;

    @Schema(description = "IDs de los estudiantes asignados a la sesión.")
    private List<Integer> idsEstudiantes;
}
