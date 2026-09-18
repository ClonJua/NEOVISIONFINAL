package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apertura de historia clínica. El consentimiento de tratamiento de datos
 * (Ley 1581) se registra al crear la cuenta del paciente, no aquí.
 */
@Getter
@Setter
@NoArgsConstructor
public class AbrirHistoriaRequest {

    private String numeroHistoria;
}
