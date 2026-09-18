package com.neovision.common.exception;

/** Se lanza cuando el correo o la contraseña del usuario son incorrectos. Retorna HTTP 401. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
