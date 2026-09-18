package com.neovision.common.exception;

/** Se lanza cuando el rol asignado en base de datos no existe. Retorna HTTP 500. */
public class RolNoEncontradoException extends RuntimeException {
    public RolNoEncontradoException(String message) {
        super(message);
    }
}
