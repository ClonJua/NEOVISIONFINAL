package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos que el administrador puede editar de un paciente.")
@Getter
@Setter
public class EditarPacienteAdminRequest {

    @Schema(description = "Nombre del paciente.", example = "Juan")
    @NotBlank
    private String nombre;

    @Schema(description = "Apellido del paciente.", example = "Pérez")
    @NotBlank
    private String apellido;

    @Schema(description = "Tipo de documento: CC, TI, RC, CE o PA.", example = "CC")
    @NotBlank
    private String tipoDocumento;

    @Schema(description = "Número de documento de identidad.", example = "1020304050")
    @NotBlank
    private String numeroDocumento;

    @Schema(description = "Fecha de nacimiento en formato YYYY-MM-DD.", example = "2000-05-15")
    @NotBlank
    private String fechaNacimiento;

    @Schema(description = "Teléfono colombiano de 10 dígitos.", example = "3001234567")
    @NotBlank
    private String telefono;

    @Schema(description = "Correo del paciente.", example = "juan.perez@gmail.com")
    @NotBlank
    @Email
    private String correo;

    @Schema(description = "Nombre del representante legal. Obligatorio si el paciente es menor de edad (TI o RC).", example = "María Pérez")
    private String representanteNombre;

    @Schema(description = "Documento del representante legal. Obligatorio si el paciente es menor de edad (TI o RC).", example = "52001234")
    private String representanteDocumento;

    @Schema(description = "Parentesco del representante legal con el paciente. Obligatorio si el paciente es menor de edad (TI o RC).", example = "Madre")
    private String representanteParentesco;
}
