package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para registrar un nuevo estudiante.")
@Getter
@Setter
public class CrearEstudianteRequest {

    @Schema(description = "Nombre del estudiante.", example = "Valeria")
    @NotBlank
    private String nombre;

    @Schema(description = "Apellido del estudiante.", example = "López")
    @NotBlank
    private String apellido;

    @Schema(description = "Correo institucional del estudiante.", example = "v.lopez@unbosque.edu.co")
    @NotBlank
    @Email
    private String correo;

    @Schema(description = "Número de cédula.", example = "1020304050")
    @NotBlank
    private String cedula;

    @Schema(description = "Semestre clinico (solo 7, 8 o 9).", example = "7")
    @NotNull
    @Min(7)
    @Max(9)
    private Integer semestre;

    @Schema(description = "Teléfono de contacto (10 dígitos).", example = "3009876543")
    private String telefono;
}
