package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para crear o actualizar un salón dentro de un espacio clínico.")
@Getter
@Setter
public class SalonRequest {
    @Schema(description = "Nombre del salón.", example = "Cubículo 1")
    @NotBlank
    private String nombre;

    @Schema(description = "ID del espacio al que pertenece el salón.", example = "2")
    @NotNull
    private Integer idEspacio;
}
