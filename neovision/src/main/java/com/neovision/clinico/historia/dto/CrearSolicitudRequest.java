package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solicitud del paciente para recibir copia de su historia clínica. */
@Getter
@Setter
@NoArgsConstructor
public class CrearSolicitudRequest {
    private String motivo;
}
