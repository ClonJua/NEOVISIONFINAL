package com.neovision.common.exception;

/** Se lanza cuando no se encuentra una solicitud de copia de historia clínica con el ID indicado. Se traduce a HTTP 404 Not Found. */
public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(String message) {
        super(message);
    }
}
