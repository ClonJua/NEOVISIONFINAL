package com.neovision.common.exception;

/**
 * Se lanza cuando una cuenta acumuló demasiados intentos fallidos de login
 * seguidos y está temporalmente bloqueada (protección contra fuerza bruta).
 * Retorna HTTP 429 (Too Many Requests).
 */
public class CuentaBloqueadaTemporalmenteException extends RuntimeException {
    public CuentaBloqueadaTemporalmenteException(String message) {
        super(message);
    }
}
