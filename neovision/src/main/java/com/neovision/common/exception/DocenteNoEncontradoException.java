package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un docente con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class DocenteNoEncontradoException extends RuntimeException {
    public DocenteNoEncontradoException(String message) {
        super(message);
    }
}
