package com.neovision.common.exception;

/**
 * Se lanza cuando un usuario autenticado intenta actuar sobre un recurso (cita,
 * bloque, etc.) que no le pertenece — equivalente genérico de
 * {@link AccesoNoAutorizadoHistoriaException} para recursos fuera de historia
 * clínica. Se traduce a HTTP 403 Forbidden.
 */
public class AccesoNoAutorizadoException extends RuntimeException {
    public AccesoNoAutorizadoException(String message) {
        super(message);
    }
}
