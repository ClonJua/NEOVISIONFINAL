package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "Datos de un espacio clínico con sus salones.")
@Getter
@AllArgsConstructor
public class EspacioResponse {
    @Schema(description = "ID único del espacio.")
    private Integer idEspacio;
    @Schema(description = "Nombre del espacio.", example = "Clínica de Optometría")
    private String nombre;
    @Schema(description = "Descripción del espacio.")
    private String descripcion;
    @Schema(description = "Indica si el espacio está activo.")
    private Boolean activo;
    @Schema(description = "Lista de salones pertenecientes a este espacio.")
    private List<SalonResponse> salones;
}
