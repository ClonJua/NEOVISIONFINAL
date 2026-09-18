package com.neovision.common.exception;

/** Se lanza cuando no se encuentra una atención con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class AtencionNoEncontradaException extends RuntimeException {
    public AtencionNoEncontradaException(String message) {
        super(message);
    }
}
