package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para crear un bloque de cita dentro de una sesión.")
@Getter
@Setter
public class CitaRequest {

    @Schema(description = "ID de la sesión a la que pertenece el bloque.", example = "1")
    @NotNull
    private Integer idSesion;

    @Schema(description = "ID del estudiante asignado al bloque.", example = "5")
    @NotNull
    private Integer idEstudiante;

    @Schema(description = "ID del segundo estudiante del bloque (los pacientes se atienden normalmente en pareja). Opcional.", example = "6")
    private Integer idEstudiante2;

    @Schema(description = "ID del paciente. Si es null, el bloque queda en estado DISPONIBLE.", example = "null")
    private Integer idPaciente;

    @Schema(description = "Observaciones opcionales.", example = "Primera visita")
    private String observaciones;
}
