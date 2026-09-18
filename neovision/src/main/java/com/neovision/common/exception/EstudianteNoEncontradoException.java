package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un estudiante con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class EstudianteNoEncontradoException extends RuntimeException {
    public EstudianteNoEncontradoException(String message) {
        super(message);
    }
}
