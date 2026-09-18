package com.neovision.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Token de acceso de Microsoft para autenticación institucional.")
@Getter
@Setter
public class MicrosoftLoginRequest {

    @Schema(description = "Access token generado por MSAL en el frontend.", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...")
    @NotBlank
    private String accessToken;
}
