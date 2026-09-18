package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Datos que el administrador puede editar de un estudiante.")
@Getter
@Setter
public class EditarEstudianteAdminRequest {

    @Schema(description = "Nombre del estudiante.", example = "Valeria")
    @NotBlank
    private String nombre;

    @Schema(description = "Apellido del estudiante.", example = "López")
    @NotBlank
    private String apellido;

    @Schema(description = "Número de cédula.", example = "1020304050")
    @NotBlank
    private String cedula;

    @Schema(description = "Semestre clinico (solo 7, 8 o 9).", example = "7")
    @NotNull
    @Min(7)
    @Max(9)
    private Integer semestre;

    @Schema(description = "Teléfono de contacto.", example = "3009876543")
    @NotBlank
    private String telefono;

    @Schema(description = "Correo institucional.", example = "v.lopez@unbosque.edu.co")
    @NotBlank
    @Email
    private String correo;
}
