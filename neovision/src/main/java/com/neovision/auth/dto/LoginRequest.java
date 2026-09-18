package com.neovision.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Credenciales para iniciar sesión en el sistema.")
@Getter
@Setter
public class LoginRequest {

    @Schema(description = "Correo registrado en el sistema.", example = "juan.perez@gmail.com")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @Schema(description = "Contraseña del usuario.", example = "Clave1234")
    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;
}
