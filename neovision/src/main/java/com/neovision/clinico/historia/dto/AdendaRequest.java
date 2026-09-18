package com.neovision.clinico.historia.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Texto de una adenda (corrección append-only sobre una atención finalizada). */
@Getter
@Setter
@NoArgsConstructor
public class AdendaRequest {

    @NotBlank(message = "El texto de la adenda es obligatorio")
    private String texto;
}
