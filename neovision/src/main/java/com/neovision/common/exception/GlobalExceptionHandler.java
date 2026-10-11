package com.neovision.common.exception;

import com.neovision.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejador global de excepciones. Convierte excepciones del dominio en respuestas
 * HTTP con el código de estado apropiado y el mensaje de error envuelto en ApiResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiResponse<Void>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CuentaBloqueadaTemporalmenteException.class)
    public ResponseEntity<ApiResponse<Void>> handleCuentaBloqueada(CuentaBloqueadaTemporalmenteException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoRegistradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioNoRegistrado(UsuarioNoRegistradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RolNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleRolNoEncontrado(RolNoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleCorreoYaRegistrado(CorreoYaRegistradoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AutenticacionMicrosoftException.class)
    public ResponseEntity<ApiResponse<Void>> handleAutenticacionMicrosoft(AutenticacionMicrosoftException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(HistoriaInmutableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHistoriaInmutable(HistoriaInmutableException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccesoNoAutorizadoHistoriaException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoNoAutorizadoHistoria(AccesoNoAutorizadoHistoriaException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ContenidoIncompletoException.class)
    public ResponseEntity<ApiResponse<Void>> handleContenidoIncompleto(ContenidoIncompletoException ex) {
        return ResponseEntity
                .status(422) // Unprocessable Entity
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AtencionNoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> handleAtencionNoEncontrada(AtencionNoEncontradaException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleEstadoInvalido(EstadoInvalidoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            DocenteNoEncontradoException.class,
            EstudianteNoEncontradoException.class,
            PacienteNoEncontradoException.class,
            EspecialidadNoEncontradaException.class,
            CitaNoEncontradaException.class,
            SesionNoEncontradaException.class,
            SolicitudNoEncontradaException.class,
            SalonNoEncontradoException.class,
            EspacioNoEncontradoException.class,
            UsuarioNoEncontradoException.class
    })
    public ResponseEntity<ApiResponse<Void>> handlePersonaNoEncontrada(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoNoAutorizado(AccesoNoAutorizadoException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentoDuplicado(DocumentoDuplicadoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecursoDuplicado(RecursoDuplicadoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Conflicto de edición concurrente (bloqueo optimista @Version): el registro
    // fue modificado por otro usuario entre la lectura y el guardado.
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "El registro fue modificado por otro usuario. Recarga la atención e intenta de nuevo."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // Denegaciones de @PreAuthorize / method security: deben ser 403, no 400.
    // (AccessDeniedException extiende RuntimeException, por eso necesita un handler propio.)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para realizar esta acción"));
    }

    // Errores de base de datos (ej. violacion de restriccion unica por una
    // condicion de carrera con doble clic) traen el SQL y el detalle nativo de
    // Postgres en el mensaje. Antes caian en el handler generico de abajo y ese
    // texto crudo se le mostraba al usuario tal cual en un popup; ahora se
    // registra el detalle en el log del servidor y se responde algo legible.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("Error de base de datos no manejado", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "No se pudo completar la operación porque entra en conflicto con datos existentes. Intenta de nuevo."));
    }

    // Fallos de envio de correo (SMTP no configurado o inalcanzable) traen el
    // stacktrace de Jakarta Mail/SSL en el mensaje; mismo problema que arriba.
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiResponse<Void>> handleMail(MailException ex) {
        log.error("Error al enviar correo", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "No se pudo enviar el correo en este momento. Intenta de nuevo más tarde o contacta al administrador."));
    }

    // Spring lo lanza antes de llegar al controller cuando el archivo de una carga
    // masiva supera spring.servlet.multipart.max-file-size; el mensaje nativo
    // menciona clases internas, asi que se responde algo legible.
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleArchivoDemasiadoGrande(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El archivo es demasiado grande (máximo 5 MB)."));
    }

    // Un parametro de ruta o query con tipo equivocado (p.ej. /citas/abc/cancelar,
    // donde se espera un Integer). El mensaje nativo de Spring menciona clases
    // internas ("Failed to convert value of type 'java.lang.String' to required
    // type 'java.lang.Integer'"), asi que se responde algo generico sin filtrar
    // detalles de implementacion.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El valor de '" + ex.getName() + "' no tiene un formato válido."));
    }

    // Cuerpo JSON malformado o ilegible. El mensaje nativo expone detalles del
    // parser (posicion, caracter, clase Jackson); se responde algo legible.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMensajeIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El cuerpo de la petición no tiene un formato válido."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
