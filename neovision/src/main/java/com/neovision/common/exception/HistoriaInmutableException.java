package com.neovision.common.exception;

/**
 * Se lanza al intentar modificar una atención ya FINALIZADA. La historia clínica
 * es inmutable una vez firmada (Resolución 1995/1999, Art. 18); las correcciones
 * solo se hacen mediante adendas. Se traduce a HTTP 409 Conflict.
 */
public class HistoriaInmutableException extends RuntimeException {
    public HistoriaInmutableException(String message) {
        super(message);
    }
}
