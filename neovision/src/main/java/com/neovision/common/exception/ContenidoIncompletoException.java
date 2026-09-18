package com.neovision.common.exception;

/**
 * Se lanza al intentar finalizar (sellar como inmutable) una atención que no
 * cumple el contenido clínico mínimo exigido (p. ej. sin motivo de consulta o
 * sin diagnóstico). Evita registros clínicos vacíos e irreversibles.
 * Se traduce a HTTP 422 Unprocessable Entity.
 */
public class ContenidoIncompletoException extends RuntimeException {
    public ContenidoIncompletoException(String message) {
        super(message);
    }
}
