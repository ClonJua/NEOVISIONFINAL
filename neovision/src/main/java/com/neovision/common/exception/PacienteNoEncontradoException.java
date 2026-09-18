package com.neovision.common.exception;

/** Se lanza cuando no se encuentra un paciente con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class PacienteNoEncontradoException extends RuntimeException {
    public PacienteNoEncontradoException(String message) {
        super(message);
    }
}
