package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos para registrar un nuevo docente.")
@Getter
@Setter
public class CrearDocenteRequest {

    @Schema(description = "Nombre del docente.", example = "Carlos")
    @NotBlank
    private String nombre;

    @Schema(description = "Apellido del docente.", example = "Ramírez")
    @NotBlank
    private String apellido;

    @Schema(description = "Correo institucional del docente.", example = "c.ramirez@unbosque.edu.co")
    @NotBlank
    @Email
    private String correo;

    @Schema(description = "Número de cédula.", example = "12345678")
    @Pattern(regexp = "^(\\d{5,15})?$", message = "debe contener solo números (5 a 15 dígitos)")
    private String cedula;

    @Schema(description = "Teléfono de contacto (10 dígitos).", example = "3001234567")
    @Pattern(regexp = "^(3\\d{9})?$", message = "debe ser un celular colombiano válido (10 dígitos, inicia con 3)")
    private String telefono;

    @Schema(description = "IDs de especialidades clínicas que el docente puede supervisar.")
    private java.util.List<Integer> idsEspecialidades;
}
