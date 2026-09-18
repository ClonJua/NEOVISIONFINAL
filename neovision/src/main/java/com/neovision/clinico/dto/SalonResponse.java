package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Datos de un salón dentro de un espacio clínico.")
@Getter
@AllArgsConstructor
public class SalonResponse {
    @Schema(description = "ID único del salón.")
    private Integer idSalon;
    @Schema(description = "Nombre del salón.", example = "Cubículo 1")
    private String nombre;
    @Schema(description = "Indica si el salón está activo.")
    private Boolean activo;
    @Schema(description = "ID del espacio al que pertenece.")
    private Integer idEspacio;
    @Schema(description = "Nombre del espacio al que pertenece.")
    private String nombreEspacio;
}
