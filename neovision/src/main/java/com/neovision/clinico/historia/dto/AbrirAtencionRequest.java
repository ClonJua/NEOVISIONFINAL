package com.neovision.clinico.historia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Abre una nueva atención (encuentro) a partir de una cita del estudiante. */
@Getter
@Setter
@NoArgsConstructor
public class AbrirAtencionRequest {

    @NotNull(message = "La cita es obligatoria")
    private Integer idCita;
}
