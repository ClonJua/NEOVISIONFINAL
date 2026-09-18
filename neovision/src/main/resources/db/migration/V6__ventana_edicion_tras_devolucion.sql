-- =============================================================================
-- V6: la devolucion del docente reabre la ventana de edicion del estudiante
-- -----------------------------------------------------------------------------
-- La ventana de oportunidad (Res. 1995/1999) se anclaba solo a la fecha de la
-- cita: fecha_cita + 2 dias. El problema es que ese reloj seguia corriendo
-- mientras la atencion esperaba en PENDIENTE_REVISION, en manos del docente.
--
-- Si el docente tardaba mas de dos dias en revisarla y la devolvia, el estudiante
-- recibia una atencion DEVUELTA que ya no podia editar: no podia corregir lo que
-- le pidieron, solo reenviarla igual. La correccion solicitada nunca se podia
-- hacer, y el par docente/estudiante quedaba en un bucle.
--
-- Con esta columna el plazo pasa a ser el mayor entre fecha_cita + 2 dias y
-- fecha_devolucion + 2 dias: el estudiante conserva su plazo completo despues de
-- cada devolucion, pero sigue acotado (no puede editar indefinidamente).
--
-- Las atenciones ya devueltas quedan con fecha_devolucion NULL: mantienen el
-- comportamiento anterior, que es el correcto para ellas (no se les puede
-- inventar una fecha de devolucion que no se registro).
-- =============================================================================

ALTER TABLE atencion ADD COLUMN IF NOT EXISTS fecha_devolucion TIMESTAMP(6);

COMMENT ON COLUMN atencion.fecha_devolucion IS
    'Momento de la ultima devolucion del docente. Reabre la ventana de edicion del estudiante: el tiempo en revision no consume su plazo';
