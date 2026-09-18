package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para que un paciente reserve un bloque eligiendo especialidad.")
@Getter
@Setter
public class ReservarBloqueRequest {

    @Schema(description = "ID de la especialidad por la cual se agenda. Si es null, se asume FUNCIONAL.")
    private Integer idEspecialidad;

    @Schema(description = "Subtipo cuando la especialidad es CONTROL: 'lentes de contacto', 'funcional', 'protesis', 'prueba ambulatoria', 'terapia'.")
    private String subtipoControl;
}
