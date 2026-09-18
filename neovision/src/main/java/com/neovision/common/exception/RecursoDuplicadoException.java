package com.neovision.common.exception;

/**
 * Se lanza cuando se intenta crear o renombrar un recurso con un nombre que ya
 * está en uso (p. ej. una especialidad con nombre repetido). Se traduce a
 * HTTP 409 Conflict.
 */
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String message) {
        super(message);
    }
}
