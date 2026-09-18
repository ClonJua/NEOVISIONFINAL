package com.neovision.common.exception;

/** Se lanza cuando no se encuentra el usuario autenticado en base de datos. Se traduce a HTTP 404 Not Found. */
public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
}
