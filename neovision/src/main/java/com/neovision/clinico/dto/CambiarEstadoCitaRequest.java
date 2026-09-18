package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para cambiar el estado de una cita.")
@Getter
@Setter
public class CambiarEstadoCitaRequest {
    @Schema(description = "Nuevo estado de la cita: COMPLETADO o CANCELADO.", example = "COMPLETADO")
    @NotBlank
    private String estado;

    @Schema(description = "Observaciones requeridas cuando el estado es CANCELADO.", example = "El paciente no asistió")
    private String observaciones;
}
