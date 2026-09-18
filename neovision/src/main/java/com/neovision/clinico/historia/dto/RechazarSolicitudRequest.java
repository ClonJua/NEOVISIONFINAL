package com.neovision.clinico.historia.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RechazarSolicitudRequest {

    @NotBlank(message = "El motivo del rechazo es obligatorio")
    private String motivo;
}
