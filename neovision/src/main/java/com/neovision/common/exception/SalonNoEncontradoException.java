package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un salón con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class SalonNoEncontradoException extends RuntimeException {
    public SalonNoEncontradoException(String message) {
        super(message);
    }
}
