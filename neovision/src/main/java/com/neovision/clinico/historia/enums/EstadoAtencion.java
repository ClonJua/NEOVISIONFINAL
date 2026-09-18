package com.neovision.clinico.historia.enums;

/**
 * Estados del ciclo de vida de una atención (historia clínica electrónica).
 *
 * <p>Flujo: BORRADOR → PENDIENTE_REVISION → (DEVUELTA → BORRADOR) → FINALIZADA.
 * Una vez FINALIZADA, la atención queda bloqueada e inmutable (Resolución 1995/1999,
 * Art. 18). Las correcciones posteriores solo se hacen mediante adendas.
 */
public enum EstadoAtencion {
    /** El estudiante está diligenciando; editable solo por su autor. */
    BORRADOR,
    /** Enviada por el estudiante; el docente supervisor debe revisarla. */
    PENDIENTE_REVISION,
    /** El docente la devolvió con observaciones; vuelve a ser editable. */
    DEVUELTA,
    /** Firmada por el docente; bloqueada e inmutable. Solo admite adendas. */
    FINALIZADA
}
