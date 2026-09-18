package com.neovision.common.exception;

/** Se lanza cuando un usuario desactivado intenta autenticarse. Retorna HTTP 403. */
public class UsuarioInactivoException extends RuntimeException {
    public UsuarioInactivoException(String message) {
        super(message);
    }
}
