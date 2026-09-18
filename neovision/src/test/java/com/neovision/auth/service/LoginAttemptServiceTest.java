package com.neovision.auth.service;

import com.neovision.common.exception.CuentaBloqueadaTemporalmenteException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();
    private static final String CORREO = "victima@unbosque.edu.co";

    @Test
    void correoSinIntentosPrevios_noEstaBloqueado() {
        assertThatCode(() -> service.verificarNoBloqueado(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void cuatroIntentosFallidos_todaviaNoBloquea() {
        for (int i = 0; i < 4; i++) service.registrarFallo(CORREO);
        assertThatCode(() -> service.verificarNoBloqueado(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void quintoIntentoFallido_bloqueaLaCuenta() {
        for (int i = 0; i < 5; i++) service.registrarFallo(CORREO);

        assertThatThrownBy(() -> service.verificarNoBloqueado(CORREO))
                .isInstanceOf(CuentaBloqueadaTemporalmenteException.class)
                .hasMessageContaining("Demasiados intentos fallidos");
    }

    @Test
    void loginExitoso_limpiaElContadorAunDespuesDeVariosFallos() {
        for (int i = 0; i < 4; i++) service.registrarFallo(CORREO);
        service.registrarExito(CORREO);

        // Si el contador no se hubiera limpiado, un solo fallo mas ya bloquearia (4+1=5).
        service.registrarFallo(CORREO);
        assertThatCode(() -> service.verificarNoBloqueado(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoEsPorCorreoYNoAfectaAOtrasCuentas() {
        for (int i = 0; i < 5; i++) service.registrarFallo(CORREO);

        assertThatCode(() -> service.verificarNoBloqueado("otro.usuario@unbosque.edu.co"))
                .doesNotThrowAnyException();
    }

    @Test
    void normalizaCorreoPorMayusculasYEspacios() {
        for (int i = 0; i < 5; i++) service.registrarFallo("  " + CORREO.toUpperCase() + "  ");

        assertThatThrownBy(() -> service.verificarNoBloqueado(CORREO))
                .isInstanceOf(CuentaBloqueadaTemporalmenteException.class);
    }

    @Test
    void mensajeDeBloqueo_incluyeMinutosRestantes() {
        for (int i = 0; i < 5; i++) service.registrarFallo(CORREO);

        assertThatThrownBy(() -> service.verificarNoBloqueado(CORREO))
                .hasMessageContaining("minuto");
    }

    @Test
    void registrarExitoSinIntentosPrevios_noFalla() {
        assertThatCode(() -> service.registrarExito("nunca.fallo@unbosque.edu.co")).doesNotThrowAnyException();
    }
}
