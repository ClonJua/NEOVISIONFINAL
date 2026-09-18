package com.neovision.common.exception;

/** Se lanza cuando no se encuentra una cita/bloque con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class CitaNoEncontradaException extends RuntimeException {
    public CitaNoEncontradaException(String message) {
        super(message);
    }
}
