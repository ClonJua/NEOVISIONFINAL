package com.neovision.common.exception;

/** Se lanza cuando no se encuentra una sesión de práctica con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class SesionNoEncontradaException extends RuntimeException {
    public SesionNoEncontradaException(String message) {
        super(message);
    }
}
