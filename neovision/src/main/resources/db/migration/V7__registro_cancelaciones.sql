-- =============================================================================
-- V7: registro de quien cancela una cita y cuando
-- -----------------------------------------------------------------------------
-- Al cancelar, el paciente cancelaba su cita y el sistema borraba de la fila el
-- paciente y la fecha de reserva. Con eso:
--   * el limite de 3 cancelaciones por paciente en 30 dias nunca se aplicaba
--     (la consulta filtraba por paciente y fecha_reserva, ya en NULL);
--   * la cita cancelada desaparecia del historial del paciente.
--
-- Ahora la fila cancelada conserva al paciente y registra fecha_cancelacion y
-- cancelada_por; el cupo se libera creando una cita DISPONIBLE nueva en el
-- mismo bloque. Solo las cancelaciones con cancelada_por = 'PACIENTE' cuentan
-- para el limite (las del estudiante o la administracion no penalizan al paciente).
--
-- Las cancelaciones anteriores quedan con ambas columnas en NULL: no se les
-- puede inventar un autor ni una fecha que no se registraron.
-- =============================================================================

ALTER TABLE cita ADD COLUMN IF NOT EXISTS fecha_cancelacion TIMESTAMP(6);
ALTER TABLE cita ADD COLUMN IF NOT EXISTS cancelada_por VARCHAR(20);

ALTER TABLE cita DROP CONSTRAINT IF EXISTS chk_cita_cancelada_por;
ALTER TABLE cita ADD CONSTRAINT chk_cita_cancelada_por
    CHECK (cancelada_por IS NULL OR cancelada_por IN ('PACIENTE', 'ESTUDIANTE', 'ADMIN'));

COMMENT ON COLUMN cita.fecha_cancelacion IS 'Momento en que se cancelo la cita (NULL si no esta cancelada o es anterior a V7)';
COMMENT ON COLUMN cita.cancelada_por     IS 'PACIENTE | ESTUDIANTE | ADMIN. Solo PACIENTE cuenta para el limite de cancelaciones';
