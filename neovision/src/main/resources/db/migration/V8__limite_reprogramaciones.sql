-- =============================================================================
-- V8: limite de reprogramaciones del paciente
-- -----------------------------------------------------------------------------
-- El paciente puede reprogramar su reserva, pero sin limite podria mover la
-- misma cita indefinidamente y bloquear cupos. Esta columna cuenta cuantas
-- veces el propio paciente ha movido la reserva actual; el contador viaja con
-- la reserva al bloque nuevo y vuelve a 0 en el bloque que queda libre. Las
-- reprogramaciones del estudiante o de la administracion no lo incrementan.
-- =============================================================================

ALTER TABLE cita ADD COLUMN IF NOT EXISTS reprogramaciones_paciente INTEGER NOT NULL DEFAULT 0;

ALTER TABLE cita DROP CONSTRAINT IF EXISTS chk_cita_reprogramaciones_paciente;
ALTER TABLE cita ADD CONSTRAINT chk_cita_reprogramaciones_paciente
    CHECK (reprogramaciones_paciente >= 0);

COMMENT ON COLUMN cita.reprogramaciones_paciente IS 'Veces que el paciente ha reprogramado la reserva actual (limite en CitaServiceImpl)';
