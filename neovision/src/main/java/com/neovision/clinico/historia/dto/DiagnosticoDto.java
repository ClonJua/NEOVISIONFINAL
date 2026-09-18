package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Diagnóstico (CIE-10) en requests y responses. */
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticoDto {
    private String codigoCie10;
    private String descripcion;
    private Integer orden;
}
