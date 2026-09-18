package com.neovision.common.exception;

/** Se lanza cuando no se encuentra una especialidad con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class EspecialidadNoEncontradaException extends RuntimeException {
    public EspecialidadNoEncontradaException(String message) {
        super(message);
    }
}
