package com.neovision.clinico.historia.service;

import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.enums.EstadoAtencion;

import java.time.LocalDate;

/**
 * Ventana de oportunidad para editar una atención (principio de oportunidad,
 * Resolución 1995/1999): los datos clínicos se registran durante la visita o
 * inmediatamente después, no semanas más tarde.
 *
 * <p>Vive aparte del servicio porque la regla la necesitan dos lados: el servicio
 * para <em>impedir</em> la edición fuera de plazo, y el mapper para <em>informar</em>
 * al estudiante hasta cuándo tiene. Duplicar el cálculo entre ambos garantizaría
 * que tarde o temprano divergieran.
 */
public final class VentanaEdicion {

    /** Días de gracia tras la fecha de referencia durante los cuales se puede editar. */
    public static final int DIAS = 2;

    private VentanaEdicion() {}

    /**
     * Último día en que el estudiante puede editar: el mayor entre la ventana que
     * abre la fecha de la cita y la que abre la última devolución del docente.
     *
     * <p>El tiempo que la atención pasa en {@link EstadoAtencion#PENDIENTE_REVISION}
     * no consume el plazo del estudiante: la demora no es suya. Antes sí lo hacía, y
     * si el docente tardaba más de {@value #DIAS} días en revisarla, el estudiante
     * recibía una atención devuelta que ya no podía corregir — solo reenviarla igual,
     * con lo que la corrección pedida nunca podía hacerse.
     *
     * @return {@code null} si la atención no cuelga de una cita y nunca fue devuelta:
     *         no hay fecha de referencia contra la cual medir, así que no aplica.
     */
    public static LocalDate limite(Atencion a) {
        LocalDate fechaCita = a.getCita() != null && a.getCita().getSesion() != null
                ? a.getCita().getSesion().getFecha() : null;

        LocalDate porCita = fechaCita != null ? fechaCita.plusDays(DIAS) : null;
        LocalDate porDevolucion = a.getFechaDevolucion() != null
                ? a.getFechaDevolucion().toLocalDate().plusDays(DIAS) : null;

        if (porCita == null) return porDevolucion;
        if (porDevolucion == null) return porCita;
        return porCita.isAfter(porDevolucion) ? porCita : porDevolucion;
    }

    /** {@code true} si hoy el estudiante todavía puede editar los campos clínicos. */
    public static boolean editablePorEstudiante(Atencion a) {
        if (a.getEstado() == EstadoAtencion.FINALIZADA
                || a.getEstado() == EstadoAtencion.PENDIENTE_REVISION) {
            return false;
        }
        LocalDate limite = limite(a);
        return limite == null || !LocalDate.now().isAfter(limite);
    }
}
