package com.neovision.auth.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class JwtServiceTest {

    private static final long UN_DIA_MS = 86_400_000L;

    @Test
    void secretoDeMenosDe32Caracteres_fallaAlArrancar() {
        assertThatThrownBy(() -> new JwtService("corto", UN_DIA_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void secretoVacio_fallaAlArrancar() {
        assertThatThrownBy(() -> new JwtService("", UN_DIA_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretoNulo_fallaAlArrancar() {
        assertThatThrownBy(() -> new JwtService(null, UN_DIA_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretoDe32CaracteresOMas_seAceptaYGeneraTokenValido() {
        JwtService service = new JwtService("a".repeat(32), UN_DIA_MS);

        String token = service.generateToken("estudiante@unbosque.edu.co", "ROLE_ESTUDIANTE");

        assertThat(service.isTokenValid(token)).isTrue();
        assertThat(service.extractCorreo(token)).isEqualTo("estudiante@unbosque.edu.co");
        assertThat(service.extractRol(token)).isEqualTo("ROLE_ESTUDIANTE");
    }

    @Test
    void dosSecretosDistintos_noPuedenValidarElTokenDelOtro() {
        JwtService servicioA = new JwtService("a".repeat(32), UN_DIA_MS);
        JwtService servicioB = new JwtService("b".repeat(32), UN_DIA_MS);

        String token = servicioA.generateToken("x@unbosque.edu.co", "ROLE_ADMIN");

        assertThat(servicioB.isTokenValid(token)).isFalse();
    }

    @Test
    void cadaTokenTieneUnJtiUnico() {
        JwtService service = new JwtService("a".repeat(32), UN_DIA_MS);

        String tokenA = service.generateToken("x@unbosque.edu.co", "ROLE_ADMIN");
        String tokenB = service.generateToken("x@unbosque.edu.co", "ROLE_ADMIN");

        assertThat(service.extractJti(tokenA)).isNotNull().isNotEqualTo(service.extractJti(tokenB));
    }

    @Test
    void extractExpiration_devuelveUnaFechaFuturaAcordeALaExpiracionConfigurada() {
        JwtService service = new JwtService("a".repeat(32), UN_DIA_MS);
        long antesDeGenerar = System.currentTimeMillis();

        String token = service.generateToken("x@unbosque.edu.co", "ROLE_ADMIN");

        long expiracion = service.extractExpiration(token).getTime();
        assertThat(expiracion).isCloseTo(antesDeGenerar + UN_DIA_MS, org.assertj.core.data.Offset.offset(5_000L));
    }
}
