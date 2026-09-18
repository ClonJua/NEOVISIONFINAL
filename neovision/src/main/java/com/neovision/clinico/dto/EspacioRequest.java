package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para crear o actualizar un espacio clínico.")
@Getter
@Setter
public class EspacioRequest {
    @Schema(description = "Nombre del espacio.", example = "Clínica de Optometría")
    @NotBlank
    private String nombre;

    @Schema(description = "Descripción opcional del espacio.", example = "Piso 3, Bloque B")
    private String descripcion;
}
