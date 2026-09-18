package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Datos del estudiante retornados por el sistema.")
@Getter
@AllArgsConstructor
public class EstudianteResponse {
    @Schema(description = "ID único del estudiante.")
    private Integer idEstudiante;
    @Schema(description = "Nombre del estudiante.")
    private String nombre;
    @Schema(description = "Apellido del estudiante.")
    private String apellido;
    @Schema(description = "Correo institucional.")
    private String correo;
    @Schema(description = "Número de cédula.")
    private String cedula;
    @Schema(description = "Semestre actual del estudiante.")
    private Integer semestre;
    @Schema(description = "Teléfono de contacto.")
    private String telefono;
    @Schema(description = "Indica si el estudiante está activo en el sistema.")
    private Boolean activo;
}
