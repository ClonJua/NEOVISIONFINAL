package com.neovision.common.exception;

/**
 * Se lanza cuando un usuario autenticado intenta una acción sobre una historia
 * clínica para la que no tiene permiso según las reglas de negocio (p. ej. un
 * estudiante editando una atención que no es suya, o un docente firmando una
 * atención de una sesión que no supervisa). Se traduce a HTTP 403 Forbidden.
 */
public class AccesoNoAutorizadoHistoriaException extends RuntimeException {
    public AccesoNoAutorizadoHistoriaException(String message) {
        super(message);
    }
}
