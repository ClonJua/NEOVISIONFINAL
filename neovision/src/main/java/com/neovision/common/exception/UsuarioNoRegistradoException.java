package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un usuario con el correo indicado. Retorna HTTP 404. */
public class UsuarioNoRegistradoException extends RuntimeException {
    public UsuarioNoRegistradoException(String message) {
        super(message);
    }
}
