package com.neovision.clinico.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resultado de verificar la cadena de hashes de la bitácora de auditoría. */
@Getter
@AllArgsConstructor
public class VerificacionCadenaResponse {
    private boolean valida;
    private long totalRegistros;
    private Long idRegistroAlterado;
    private String mensaje;
}
