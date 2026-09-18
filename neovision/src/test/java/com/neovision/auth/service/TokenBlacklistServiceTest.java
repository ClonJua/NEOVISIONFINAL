package com.neovision.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;


class TokenBlacklistServiceTest {

    private final TokenBlacklistService service = new TokenBlacklistService();
    private static final String JTI = "11111111-1111-1111-1111-111111111111";

    @Test
    void jtiNuncaRevocado_noEstaRevocado() {
        assertThat(service.estaRevocado(JTI)).isFalse();
    }

    @Test
    void jtiRevocado_quedaRevocadoMientrasNoExpire() {
        service.revocar(JTI, Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(service.estaRevocado(JTI)).isTrue();
    }

    @Test
    void jtiRevocado_dejaDeEstarRevocadoTrasSuPropiaExpiracion() {
        service.revocar(JTI, Instant.now().minus(1, ChronoUnit.SECONDS));

        assertThat(service.estaRevocado(JTI)).isFalse();
    }

    @Test
    void laRevocacionEsPorJtiYNoAfectaAOtrosTokens() {
        service.revocar(JTI, Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(service.estaRevocado("22222222-2222-2222-2222-222222222222")).isFalse();
    }

    @Test
    void jtiNulo_nuncaSeConsideraRevocado() {
        assertThat(service.estaRevocado(null)).isFalse();
    }

    @Test
    void revocarConJtiNulo_noFalla() {
        service.revocar(null, Instant.now().plus(1, ChronoUnit.HOURS));
    }
}
