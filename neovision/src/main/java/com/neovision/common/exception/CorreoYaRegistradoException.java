package com.neovision.common.exception;

/** Se lanza cuando se intenta registrar un correo que ya existe en el sistema. Retorna HTTP 409. */
public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String message) {
        super(message);
    }
}
