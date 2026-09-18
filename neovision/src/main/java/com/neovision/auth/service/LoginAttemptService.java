package com.neovision.auth.service;

import com.neovision.common.exception.CuentaBloqueadaTemporalmenteException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_BLOQUEO_MINUTOS = 15;

    private static class Intentos {
        int conteo;
        LocalDateTime bloqueadoHasta;
    }

    private final Map<String, Intentos> porCorreo = new ConcurrentHashMap<>();

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    /** Lanza {@link CuentaBloqueadaTemporalmenteException} si la cuenta sigue bloqueada; si la ventana ya expiró, la resetea. */
    public void verificarNoBloqueado(String correo) {
        Intentos i = porCorreo.get(normalizar(correo));
        if (i == null) return;
        synchronized (i) {
            if (i.bloqueadoHasta == null) return;
            LocalDateTime ahora = LocalDateTime.now();
            if (ahora.isBefore(i.bloqueadoHasta)) {
                long minutosRestantes = ChronoUnit.MINUTES.between(ahora, i.bloqueadoHasta) + 1;
                throw new CuentaBloqueadaTemporalmenteException(
                        "Demasiados intentos fallidos. Intenta de nuevo en " + minutosRestantes + " minuto(s).");
            }
            // La ventana de bloqueo ya expiró: se resetea el contador.
            i.conteo = 0;
            i.bloqueadoHasta = null;
        }
    }

    /** Registra un intento fallido; bloquea la cuenta si alcanza el máximo. */
    public void registrarFallo(String correo) {
        Intentos i = porCorreo.computeIfAbsent(normalizar(correo), k -> new Intentos());
        synchronized (i) {
            i.conteo++;
            if (i.conteo >= MAX_INTENTOS) {
                i.bloqueadoHasta = LocalDateTime.now().plusMinutes(VENTANA_BLOQUEO_MINUTOS);
            }
        }
    }

    /** Limpia el contador tras un login exitoso. */
    public void registrarExito(String correo) {
        porCorreo.remove(normalizar(correo));
    }
}
