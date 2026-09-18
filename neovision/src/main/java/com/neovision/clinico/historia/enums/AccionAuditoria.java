package com.neovision.clinico.historia.enums;

/**
 * Tipos de acción registrados en la bitácora de auditoría (append-only) del
 * módulo de historia clínica. Garantiza la trazabilidad exigida (HU05) e incluye
 * los accesos de lectura (CONSULTAR) por tratarse de datos sensibles (Ley 1581).
 */
public enum AccionAuditoria {
    CREAR_HISTORIA,
    REGISTRAR_CONSENTIMIENTO,
    ABRIR_ATENCION,
    GUARDAR_BORRADOR,
    ENVIAR_REVISION,
    DEVOLVER,
    FINALIZAR,
    CREAR_ADENDA,
    CONSULTAR
}
