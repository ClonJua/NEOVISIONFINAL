package com.neovision.common.exception;

/**
 * Se lanza cuando el número de documento/cédula que se intenta registrar o
 * actualizar ya está en uso por otra persona del mismo tipo (docente,
 * estudiante o paciente). Se traduce a HTTP 409 Conflict.
 */
public class DocumentoDuplicadoException extends RuntimeException {
    public DocumentoDuplicadoException(String message) {
        super(message);
    }
}
