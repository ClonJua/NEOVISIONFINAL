package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
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
    private String cedula;

    @Schema(description = "Teléfono de contacto.", example = "3001234567")
    @NotBlank
    private String telefono;

    @Schema(description = "Correo institucional.", example = "c.ramirez@unbosque.edu.co")
    @NotBlank
    @Email
    private String correo;

    @Schema(description = "IDs de especialidades clínicas que el docente puede supervisar.")
    private java.util.List<Integer> idsEspecialidades;
}
