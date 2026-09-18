package com.neovision.admin.dto;

import com.neovision.clinico.dto.EspecialidadResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "Datos del docente retornados por el sistema.")
@Getter
@AllArgsConstructor
public class DocenteResponse {
    @Schema(description = "ID único del docente.")
    private Integer idDocente;
    @Schema(description = "Nombre del docente.")
    private String nombre;
    @Schema(description = "Apellido del docente.")
    private String apellido;
    @Schema(description = "Correo institucional.")
    private String correo;
    @Schema(description = "Número de cédula.")
    private String cedula;
    @Schema(description = "Teléfono de contacto.")
    private String telefono;
    @Schema(description = "Indica si el docente está activo en el sistema.")
    private Boolean activo;
    @Schema(description = "Especialidades clínicas que el docente puede supervisar.")
    private List<EspecialidadResponse> especialidades;
}
