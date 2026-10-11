package com.neovision.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cobertura de que cada excepción de dominio se traduzca al código HTTP
 * correcto. El frontend distingue "no encontrado" (404) de "conflicto" (409)
 * de "no autorizado" (403), etc. a partir de este código — un handler mal
 * registrado (o una excepción nueva sin registrar aquí, cayendo al catch-all
 * de RuntimeException → 400) rompe esa semántica en silencio.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void credencialesInvalidas_mapeaA401() {
        assertStatus(handler.handleCredencialesInvalidas(new CredencialesInvalidasException("x")), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cuentaBloqueadaTemporalmente_mapeaA429() {
        assertStatus(handler.handleCuentaBloqueada(new CuentaBloqueadaTemporalmenteException("x")), HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void usuarioNoRegistrado_mapeaA404() {
        assertStatus(handler.handleUsuarioNoRegistrado(new UsuarioNoRegistradoException("x")), HttpStatus.NOT_FOUND);
    }

    @Test
    void rolNoEncontrado_esErrorDeConfiguracionInterna_mapeaA500() {
        assertStatus(handler.handleRolNoEncontrado(new RolNoEncontradoException("x")), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void usuarioInactivo_mapeaA403() {
        assertStatus(handler.handleUsuarioInactivo(new UsuarioInactivoException("x")), HttpStatus.FORBIDDEN);
    }

    @Test
    void correoYaRegistrado_mapeaA409() {
        assertStatus(handler.handleCorreoYaRegistrado(new CorreoYaRegistradoException("x")), HttpStatus.CONFLICT);
    }

    @Test
    void autenticacionMicrosoft_mapeaA400() {
        assertStatus(handler.handleAutenticacionMicrosoft(new AutenticacionMicrosoftException("x")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void historiaInmutable_mapeaA409() {
        assertStatus(handler.handleHistoriaInmutable(new HistoriaInmutableException("x")), HttpStatus.CONFLICT);
    }

    @Test
    void accesoNoAutorizadoHistoria_mapeaA403() {
        assertStatus(handler.handleAccesoNoAutorizadoHistoria(new AccesoNoAutorizadoHistoriaException("x")), HttpStatus.FORBIDDEN);
    }

    @Test
    void contenidoIncompleto_mapeaA422() {
        assertThat(handler.handleContenidoIncompleto(new ContenidoIncompletoException("x")).getStatusCode().value())
                .isEqualTo(422);
    }

    @Test
    void atencionNoEncontrada_mapeaA404() {
        assertStatus(handler.handleAtencionNoEncontrada(new AtencionNoEncontradaException("x")), HttpStatus.NOT_FOUND);
    }

    @Test
    void estadoInvalido_mapeaA409() {
        assertStatus(handler.handleEstadoInvalido(new EstadoInvalidoException("x")), HttpStatus.CONFLICT);
    }

    /** Las 10 excepciones "no encontrado" comparten un único handler agrupado: las 10 deben dar 404. */
    @Test
    void todasLasExcepcionesNoEncontrado_comparteHandlerYMapeanA404() {
        List<RuntimeException> noEncontrado = List.of(
                new DocenteNoEncontradoException("x"),
                new EstudianteNoEncontradoException("x"),
                new PacienteNoEncontradoException("x"),
                new EspecialidadNoEncontradaException("x"),
                new CitaNoEncontradaException("x"),
                new SesionNoEncontradaException("x"),
                new SolicitudNoEncontradaException("x"),
                new SalonNoEncontradoException("x"),
                new EspacioNoEncontradoException("x"),
                new UsuarioNoEncontradoException("x")
        );
        for (RuntimeException ex : noEncontrado) {
            assertStatus(handler.handlePersonaNoEncontrada(ex), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void accesoNoAutorizado_mapeaA403() {
        assertStatus(handler.handleAccesoNoAutorizado(new AccesoNoAutorizadoException("x")), HttpStatus.FORBIDDEN);
    }

    @Test
    void documentoDuplicado_mapeaA409() {
        assertStatus(handler.handleDocumentoDuplicado(new DocumentoDuplicadoException("x")), HttpStatus.CONFLICT);
    }

    @Test
    void recursoDuplicado_mapeaA409() {
        assertStatus(handler.handleRecursoDuplicado(new RecursoDuplicadoException("x")), HttpStatus.CONFLICT);
    }

    @Test
    void bloqueoOptimista_deEdicionConcurrente_mapeaA409() {
        assertStatus(handler.handleOptimisticLock(
                new org.springframework.orm.ObjectOptimisticLockingFailureException("Atencion", 1)), HttpStatus.CONFLICT);
    }

    @Test
    void accessDenied_deSpringSecurity_mapeaA403NoAlCatchAll400() {
        // AccessDeniedException extiende RuntimeException: sin este handler propio caería
        // en el catch-all (400) en vez del 403 que corresponde a una denegación de @PreAuthorize.
        assertStatus(handler.handleAccessDenied(new AccessDeniedException("x")), HttpStatus.FORBIDDEN);
    }

    @Test
    void excepcionSinTipoRegistrado_caeEnElCatchAllYMapeaA400() {
        assertStatus(handler.handleRuntimeException(new RuntimeException("x")), HttpStatus.BAD_REQUEST);
    }

    /**
     * Antes DataIntegrityViolationException (SQL con el detalle nativo de Postgres,
     * ej. una violacion de restriccion unica por doble clic) caia en el catch-all
     * de RuntimeException y ese texto crudo se le mostraba al usuario en un popup.
     */
    @Test
    void errorDeBaseDeDatos_mapeaA409SinExponerElDetalleCrudoDeSql() {
        var ex = new org.springframework.dao.DataIntegrityViolationException(
                "could not execute statement [ERROR: llave duplicada viola restriccion de unicidad ...]");
        var response = handler.handleDataAccess(ex);

        assertStatus(response, HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage())
                .doesNotContain("could not execute statement")
                .doesNotContain("llave duplicada");
    }

    /** Igual que el caso anterior, pero para fallos de envio de correo (SMTP no configurado, SSL, etc.). */
    @Test
    void errorDeCorreo_mapeaA503SinExponerElDetalleCrudoDeSmtp() {
        var ex = new org.springframework.mail.MailSendException(
                "Mail server connection failed: SSLHandshakeException: PKIX path validation failed");
        var response = handler.handleMail(ex);

        assertStatus(response, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getMessage())
                .doesNotContain("SSLHandshakeException")
                .doesNotContain("PKIX");
    }

    @Test
    void validacionDeCampos_mapeaA400ConElPrimerErrorDeCampo() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("request", "correo", "no puede estar vacío");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));
        MethodParameter param = mock(MethodParameter.class);

        var response = handler.handleValidationException(new MethodArgumentNotValidException(param, bindingResult));

        assertStatus(response, HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("correo").contains("no puede estar vacío");
    }

    @Test
    void tipoDeParametroInvalido_mapeaA400SinExponerTiposInternos() {
        MethodParameter param = mock(MethodParameter.class);
        var ex = new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                "abc", Integer.class, "idCita", param, new IllegalArgumentException("x"));

        var response = handler.handleTypeMismatch(ex);

        assertStatus(response, HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .contains("idCita")
                .doesNotContain("java.lang")
                .doesNotContain("Integer");
    }

    @Test
    void cuerpoJsonIlegible_mapeaA400SinExponerDetallesDelParser() {
        var ex = new org.springframework.http.converter.HttpMessageNotReadableException(
                "JSON parse error: Unexpected character ('n' (code 110)): com.fasterxml.jackson...",
                (org.springframework.http.HttpInputMessage) null);

        var response = handler.handleMensajeIlegible(ex);

        assertStatus(response, HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .doesNotContain("Jackson")
                .doesNotContain("com.fasterxml")
                .doesNotContain("parse error");
    }

    private void assertStatus(org.springframework.http.ResponseEntity<?> response, HttpStatus esperado) {
        assertThat(response.getStatusCode()).isEqualTo(esperado);
    }
}
