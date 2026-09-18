package com.neovision.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Token de notificaciones push (Expo) del dispositivo del usuario.")
@Getter
@Setter
public class PushTokenRequest {

    @Schema(description = "Expo push token. Ej: ExponentPushToken[xxxxxxxx]", example = "ExponentPushToken[abc123]")
    @NotBlank(message = "El token es obligatorio")
    private String token;
}
