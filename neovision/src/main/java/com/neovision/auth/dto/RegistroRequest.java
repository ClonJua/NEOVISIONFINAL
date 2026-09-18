package com.neovision.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para registrar un nuevo paciente en el sistema.")
@Getter
@Setter
public class RegistroRequest {

    @Schema(description = "Nombre del paciente.", example = "Juan")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Apellido del paciente.", example = "Pérez")
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @Schema(description = "Correo del paciente.", example = "juan.perez@gmail.com")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @Schema(description = "Contraseña. Mínimo 12 caracteres.", example = "ClaveSegura2026")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
    private String contrasena;

    @Schema(description = "Tipo de documento: CC, TI, RC, CE o PA.", example = "CC")
    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;

    @Schema(description = "Número de documento de identidad.", example = "1020304050")
    @NotBlank(message = "El numero de documento es obligatorio")
    private String numeroDocumento;

    @Schema(description = "Fecha de nacimiento en formato YYYY-MM-DD.", example = "2000-05-15")
    @NotBlank(message = "La fecha de nacimiento es obligatoria")
    private String fechaNacimiento;

    @Schema(description = "Teléfono colombiano de 10 dígitos.", example = "3001234567")
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    @Schema(description = "Aceptación del tratamiento de datos personales (Ley 1581/2012).", example = "true")
    private Boolean autorizaTratamientoDatos;

    @Schema(description = "Nombre del representante legal. Obligatorio si el paciente es menor de edad (TI o RC).", example = "María Pérez")
    private String representanteNombre;

    @Schema(description = "Documento del representante legal. Obligatorio si el paciente es menor de edad (TI o RC).", example = "52001234")
    private String representanteDocumento;

    @Schema(description = "Parentesco del representante legal con el paciente. Obligatorio si el paciente es menor de edad (TI o RC).", example = "Madre")
    private String representanteParentesco;
}
