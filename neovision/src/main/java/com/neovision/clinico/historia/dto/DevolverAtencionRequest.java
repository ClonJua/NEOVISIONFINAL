package com.neovision.clinico.historia.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Devolución de la atención al estudiante con el motivo de la corrección. */
@Getter
@Setter
@NoArgsConstructor
public class DevolverAtencionRequest {

    @NotBlank(message = "El motivo de la devolución es obligatorio")
    private String motivo;
}
