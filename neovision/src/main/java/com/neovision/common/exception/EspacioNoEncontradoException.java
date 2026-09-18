package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un espacio con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class EspacioNoEncontradoException extends RuntimeException {
    public EspacioNoEncontradoException(String message) {
        super(message);
    }
}
