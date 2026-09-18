package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Procedimiento (CUPS) en requests y responses. */
@Getter
@Setter
@NoArgsConstructor
public class ProcedimientoDto {
    private String codigoCups;
    private String descripcion;
    private Integer orden;
}
