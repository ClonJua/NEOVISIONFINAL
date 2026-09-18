package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Finalización (firma) de la atención por el docente. Firmas en PNG base64 (opcionales). */
@Getter
@Setter
@NoArgsConstructor
public class FinalizarAtencionRequest {
    private String firmaDocente;
    private String firmaPaciente;
}
