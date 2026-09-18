package com.neovision.common.exception;

/**
 * Se lanza al intentar una transición de estado sobre una atención que no aplica
 * en su estado actual (p. ej. devolver o finalizar una atención que no está en
 * revisión, o reenviar una que ya está en revisión). A diferencia de
 * {@link HistoriaInmutableException}, no implica que el registro sea inmutable,
 * solo que la operación no corresponde al estado del flujo. Se traduce a HTTP 409 Conflict.
 */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String message) {
        super(message);
    }
}
