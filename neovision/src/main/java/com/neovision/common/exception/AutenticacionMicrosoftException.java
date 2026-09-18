package com.neovision.common.exception;

/** Se lanza cuando el accessToken de Microsoft es inválido o no puede verificarse. Retorna HTTP 400. */
public class AutenticacionMicrosoftException extends RuntimeException {
    public AutenticacionMicrosoftException(String message) {
        super(message);
    }
}
