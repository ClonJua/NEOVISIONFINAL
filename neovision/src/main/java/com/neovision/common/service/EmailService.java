package com.neovision.common.service;

/**
 * Servicio de envío de correos electrónicos del sistema.
 * Notifica a pacientes, estudiantes y docentes sobre eventos de citas.
 */
public interface EmailService {

    /**
     * Envía confirmación de reserva al paciente, estudiante y docente.
     *
     * @param correoPaciente   correo del paciente
     * @param nombrePaciente   nombre del paciente
     * @param correoEstudiante correo del estudiante
     * @param nombreEstudiante nombre del estudiante
     * @param correoDocente    correo del docente
     * @param nombreDocente    nombre del docente
     * @param fecha            fecha de la cita (YYYY-MM-DD)
     * @param horaInicio       hora de inicio
     * @param horaFin          hora de fin
     * @param salon            nombre del salón
     * @param espacio          nombre del espacio clínico
     */
    void enviarConfirmacionReserva(String correoPaciente, String nombrePaciente,
                                   String correoEstudiante, String nombreEstudiante,
                                   String correoDocente, String nombreDocente,
                                   String fecha, String horaInicio, String horaFin,
                                   String salon, String espacio,
                                   String especialidad, String observacionRecordatorio);

    /**
     * Envía notificación de cancelación al paciente, estudiante y docente.
     *
     * @param canceladoPor indica quién canceló (ej. "PACIENTE", "ADMIN")
     */
    void enviarCancelacion(String correoPaciente, String nombrePaciente,
                           String correoEstudiante, String nombreEstudiante,
                           String correoDocente, String nombreDocente,
                           String fecha, String horaInicio, String horaFin,
                           String canceladoPor);

    /**
     * Envía notificación de reprogramación con la cita anterior y la nueva.
     *
     * @param fechaAnterior fecha original de la cita
     * @param horaAnterior  hora original
     * @param fechaNueva    nueva fecha de la cita
     * @param horaNueva     nueva hora
     */
    void enviarReprogramacion(String correoPaciente, String nombrePaciente,
                              String correoEstudiante, String nombreEstudiante,
                              String correoDocente, String nombreDocente,
                              String fechaAnterior, String horaAnterior,
                              String fechaNueva, String horaNueva,
                              String salon, String espacio);

    /**
     * Envía notificación al paciente y estudiante cuando la cita se marca como completada.
     */
    void enviarCompletada(String correoPaciente, String nombrePaciente,
                          String correoEstudiante, String nombreEstudiante,
                          String fecha, String horaInicio, String horaFin);

    /**
     * Notifica al paciente que un estudiante lo asignó directamente a un bloque.
     */
    void enviarAsignacionPaciente(String correoPaciente, String nombrePaciente,
                                  String correoEstudiante, String nombreEstudiante,
                                  String fecha, String horaInicio, String horaFin,
                                  String salon, String espacio);

    /**
     * Envía recordatorio de cita próxima al paciente, estudiante y docente.
     *
     * @param tiempoRestante texto que indica cuánto falta (ej. "24 horas", "1 hora")
     */
    void enviarRecordatorioCita(String correoPaciente, String nombrePaciente,
                                String correoEstudiante, String nombreEstudiante,
                                String correoDocente, String nombreDocente,
                                String fecha, String horaInicio, String horaFin,
                                String salon, String espacio, String tiempoRestante,
                                String especialidad, String observacionRecordatorio);

    /**
     * Envía al paciente una copia de su historia clínica (PDF adjunto), tras la
     * aprobación de su solicitud (derecho de acceso — Ley 1266/2008).
     */
    void enviarHistoriaClinica(String correoPaciente, String nombrePaciente,
                               byte[] pdf, String nombreArchivo);

    /**
     * Envía un archivo adjunto (reporte exportado) a un correo indicado por el
     * administrador. De uso general: exportes de citas u otros reportes operativos.
     */
    void enviarReporteAdjunto(String correoDestino, String asunto, String descripcion,
                              String nombreArchivo, byte[] contenido, String contentType);
}
