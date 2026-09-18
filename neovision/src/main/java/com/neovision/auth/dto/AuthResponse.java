package com.neovision.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Respuesta retornada tras un login o registro exitoso.")
@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    @Schema(description = "Token JWT para autenticar las siguientes peticiones.", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Rol del usuario: ROLE_ADMIN, ROLE_DOCENTE, ROLE_ESTUDIANTE o ROLE_PACIENTE.", example = "ROLE_PACIENTE")
    private String rol;

    @Schema(description = "Correo del usuario autenticado.", example = "juan.perez@gmail.com")
    private String correo;
}
