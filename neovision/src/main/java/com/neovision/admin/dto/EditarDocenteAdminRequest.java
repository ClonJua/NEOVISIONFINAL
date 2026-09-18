package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos que el administrador puede editar de un docente.")
@Getter
@Setter
public class EditarDocenteAdminRequest {

    @Schema(description = "Nombre del docente.", example = "Carlos")
    @NotBlank
    private String nombre;

    @Schema(description = "Apellido del docente.", example = "Ramírez")
    @NotBlank
    private String apellido;

    @Schema(description = "Número de cédula.", example = "12345678")
    @NotBlank
    @Pattern(regexp = "^\\d{5,15}$", message = "debe contener solo números (5 a 15 dígitos)")
    private String cedula;

    @Schema(description = "Teléfono de contacto.", example = "3001234567")
    @NotBlank
    @Pattern(regexp = "^3\\d{9}$", message = "debe ser un celular colombiano válido (10 dígitos, inicia con 3)")
    private String telefono;

    @Schema(description = "Correo institucional.", example = "c.ramirez@unbosque.edu.co")
    @NotBlank
    @Email
    private String correo;

    @Schema(description = "IDs de especialidades clínicas que el docente puede supervisar.")
    private java.util.List<Integer> idsEspecialidades;
}
