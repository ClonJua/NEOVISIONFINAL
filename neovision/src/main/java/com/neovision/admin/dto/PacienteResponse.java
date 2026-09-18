package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Datos del paciente retornados por el sistema.")
@Getter
@AllArgsConstructor
public class PacienteResponse {
    @Schema(description = "ID único del paciente.")
    private Integer idPaciente;
    @Schema(description = "Nombre del paciente.")
    private String nombre;
    @Schema(description = "Apellido del paciente.")
    private String apellido;
    @Schema(description = "Correo del paciente.")
    private String correo;
    @Schema(description = "Tipo de documento: CC, TI, RC, CE o PA.")
    private String tipoDocumento;
    @Schema(description = "Número de documento de identidad.")
    private String numeroDocumento;
    @Schema(description = "Fecha de nacimiento en formato YYYY-MM-DD.")
    private String fechaNacimiento;
    @Schema(description = "Teléfono de contacto.")
    private String telefono;
    @Schema(description = "Indica si el paciente está activo en el sistema.")
    private Boolean activo;
    @Schema(description = "Indica si el paciente autorizó el tratamiento de sus datos (Ley 1581).")
    private Boolean autorizaTratamientoDatos;
    @Schema(description = "Nombre del representante legal (menores de edad).")
    private String representanteNombre;
    @Schema(description = "Documento del representante legal (menores de edad).")
    private String representanteDocumento;
    @Schema(description = "Parentesco del representante legal con el paciente (menores de edad).")
    private String representanteParentesco;
}
