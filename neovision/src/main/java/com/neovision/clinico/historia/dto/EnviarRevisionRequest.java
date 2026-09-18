package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Envío de la atención a revisión del docente. Firma del estudiante (PNG base64) opcional. */
@Getter
@Setter
@NoArgsConstructor
public class EnviarRevisionRequest {
    private String firmaEstudiante;
}
