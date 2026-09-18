package com.neovision.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class GmailEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.from-name:NeoVision}")
    private String fromName;

    private final JavaMailSender mailSender;

    public GmailEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void enviarConfirmacionReserva(String correoPaciente, String nombrePaciente,
                                          String correoEstudiante, String nombreEstudiante,
                                          String correoDocente, String nombreDocente,
                                          String fecha, String horaInicio, String horaFin,
                                          String salon, String espacio,
                                          String especialidad, String observacionRecordatorio) {
        try {
            String html = plantillaBase("Cita Confirmada",
                    "<p>Se ha confirmado una cita en <strong>NeoVision</strong>:</p>"
                    + detallesCita(fecha, horaInicio, horaFin, salon, espacio,
                                   nombreEstudiante, nombreDocente, nombrePaciente, especialidad)
                    + bloqueRecordatorio(observacionRecordatorio));

            enviar(correoPaciente, "NeoVision - Confirmacion de tu cita (" + fecha + ")", html);
            enviar(correoEstudiante, "NeoVision - Nueva cita asignada (" + fecha + ")", html);
            enviar(correoDocente, "NeoVision - Nueva cita en tu sesion (" + fecha + ")", html);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de confirmacion de reserva: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void enviarCancelacion(String correoPaciente, String nombrePaciente,
                                  String correoEstudiante, String nombreEstudiante,
                                  String correoDocente, String nombreDocente,
                                  String fecha, String horaInicio, String horaFin,
                                  String canceladoPor) {
        try {
            String html = plantillaBase("Cita Cancelada",
                    "<p>Se ha cancelado una cita en <strong>NeoVision</strong>.</p>"
                    + "<p><strong>Cancelado por:</strong> " + canceladoPor + "</p>"
                    + "<table style='margin:16px 0;border-collapse:collapse'>"
                    + fila("Fecha", fecha)
                    + fila("Horario", horaInicio + " - " + horaFin)
                    + fila("Paciente", nombrePaciente)
                    + fila("Estudiante", nombreEstudiante)
                    + "</table>"
                    + "<p style='color:#64748b;font-size:13px'>El bloque ha quedado disponible para otros pacientes.</p>");

            if (correoPaciente != null) {
                enviar(correoPaciente, "NeoVision - Tu cita ha sido cancelada (" + fecha + ")", html);
            }
            enviar(correoEstudiante, "NeoVision - Cita cancelada (" + fecha + ")", html);
            enviar(correoDocente, "NeoVision - Cita cancelada en tu sesion (" + fecha + ")", html);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de cancelacion: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void enviarReprogramacion(String correoPaciente, String nombrePaciente,
                                     String correoEstudiante, String nombreEstudiante,
                                     String correoDocente, String nombreDocente,
                                     String fechaAnterior, String horaAnterior,
                                     String fechaNueva, String horaNueva,
                                     String salon, String espacio) {
        try {
            String html = plantillaBase("Cita Reprogramada",
                    "<p>Se ha reprogramado una cita en <strong>NeoVision</strong>:</p>"
                    + "<table style='margin:16px 0;border-collapse:collapse'>"
                    + fila("Anterior", fechaAnterior + " a las " + horaAnterior)
                    + fila("Nueva fecha", fechaNueva + " a las " + horaNueva)
                    + fila("Lugar", espacio + " - " + salon)
                    + fila("Paciente", nombrePaciente)
                    + fila("Estudiante", nombreEstudiante)
                    + fila("Docente", nombreDocente)
                    + "</table>");

            enviar(correoPaciente, "NeoVision - Tu cita ha sido reprogramada", html);
            enviar(correoEstudiante, "NeoVision - Cita reprogramada (" + fechaNueva + ")", html);
            enviar(correoDocente, "NeoVision - Cita reprogramada en tu sesion (" + fechaNueva + ")", html);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de reprogramacion: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void enviarCompletada(String correoPaciente, String nombrePaciente,
                                 String correoEstudiante, String nombreEstudiante,
                                 String fecha, String horaInicio, String horaFin) {
        try {
            String html = plantillaBase("Cita Completada",
                    "<p>La cita ha sido marcada como <strong>completada</strong>.</p>"
                    + "<table style='margin:16px 0;border-collapse:collapse'>"
                    + fila("Fecha", fecha)
                    + fila("Horario", horaInicio + " - " + horaFin)
                    + fila("Paciente", nombrePaciente)
                    + fila("Estudiante", nombreEstudiante)
                    + "</table>"
                    + "<p style='color:#64748b;font-size:13px'>Gracias por confiar en NeoVision.</p>");

            enviar(correoPaciente, "NeoVision - Resumen de tu cita (" + fecha + ")", html);
            enviar(correoEstudiante, "NeoVision - Cita completada (" + fecha + ")", html);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de completada: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void enviarAsignacionPaciente(String correoPaciente, String nombrePaciente,
                                         String correoEstudiante, String nombreEstudiante,
                                         String fecha, String horaInicio, String horaFin,
                                         String salon, String espacio) {
        try {
            String htmlPaciente = plantillaBase("Cita Programada",
                    "<p>Se te ha asignado una cita en <strong>NeoVision</strong>:</p>"
                    + detallesCita(fecha, horaInicio, horaFin, salon, espacio, nombreEstudiante, null, nombrePaciente, null));

            enviar(correoPaciente, "NeoVision - Tienes una nueva cita (" + fecha + ")", htmlPaciente);

            String htmlEstudiante = plantillaBase("Paciente Asignado",
                    "<p>Has asignado un paciente a tu bloque:</p>"
                    + "<table style='margin:16px 0;border-collapse:collapse'>"
                    + fila("Fecha", fecha)
                    + fila("Horario", horaInicio + " - " + horaFin)
                    + fila("Paciente", nombrePaciente)
                    + "</table>");

            enviar(correoEstudiante, "NeoVision - Paciente asignado a tu bloque (" + fecha + ")", htmlEstudiante);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de asignacion paciente: {}", e.getMessage(), e);
        }
    }

    // NO @Async a propósito: RecordatorioScheduler solo marca recordatorio24hEnviado/
    // recordatorio1hEnviado en true DESPUES de que esta llamada retorna sin lanzar
    // excepcion (ver su try/catch por cita). Si fuera async y tragara el error, el
    // scheduler marcaria "enviado" sin saber si realmente se envio — y como la
    // query de recordatorios pendientes filtra por ese flag, ese recordatorio
    // jamas se reintentaria ni llegaria al paciente.
    @Override
    public void enviarRecordatorioCita(String correoPaciente, String nombrePaciente,
                                       String correoEstudiante, String nombreEstudiante,
                                       String correoDocente, String nombreDocente,
                                       String fecha, String horaInicio, String horaFin,
                                       String salon, String espacio, String tiempoRestante,
                                       String especialidad, String observacionRecordatorio) {
        try {
            String html = plantillaBase("Recordatorio de Cita - " + tiempoRestante,
                    "<p>Recordatorio: Tu cita en <strong>NeoVision</strong> se aproxima.</p>"
                    + detallesCita(fecha, horaInicio, horaFin, salon, espacio,
                                   nombreEstudiante, nombreDocente, nombrePaciente, especialidad)
                    + bloqueRecordatorio(observacionRecordatorio)
                    + "<p style='color:#ea580c;font-weight:600;margin-top:16px'>⏰ " + tiempoRestante + "</p>");

            enviar(correoPaciente, "NeoVision - Recordatorio: Tu cita en " + tiempoRestante, html);
            enviar(correoEstudiante, "NeoVision - Recordatorio: Cita en " + tiempoRestante, html);
            enviar(correoDocente, "NeoVision - Recordatorio: Cita en " + tiempoRestante, html);
        } catch (Exception e) {
            log.error("GMAIL: Error preparando email de recordatorio: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el recordatorio de la cita.");
        }
    }

    // NO @Async a propósito: SolicitudHistoriaService.aprobar() marca la solicitud
    // como cumplida inmediatamente después de llamar este método. Si fuera
    // asincrono, la solicitud quedaria "aprobada" aunque el envio fallara en
    // segundo plano — inaceptable para una solicitud de derecho de acceso
    // (Habeas Data). Por eso ademas propaga la excepcion en vez de solo loguearla:
    // el llamador debe enterarse del fallo para no marcar como resuelto algo
    // que el paciente nunca recibio.
    @Override
    public void enviarHistoriaClinica(String correoPaciente, String nombrePaciente,
                                      byte[] pdf, String nombreArchivo) {
        if (correoPaciente == null || correoPaciente.isBlank()) {
            throw new RuntimeException("No se pudo enviar: el paciente no tiene correo registrado");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(correoPaciente);
            helper.setSubject("NeoVision - Copia de tu historia clínica");
            String html = plantillaBase("Copia de Historia Clínica",
                    "<p>Hola <strong>" + nombrePaciente + "</strong>,</p>"
                    + "<p>Adjuntamos la copia de tu historia clínica, conforme a tu derecho de acceso "
                    + "a la información (Ley 1266 de 2008 y Resolución 1995 de 1999).</p>"
                    + "<p>Este documento es confidencial y de uso personal.</p>");
            helper.setText(html, true);
            helper.addAttachment(nombreArchivo,
                    new org.springframework.core.io.ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
            log.info("GMAIL: Historia clinica enviada a {}", correoPaciente);
        } catch (Exception e) {
            log.error("GMAIL: Error enviando historia clínica a {}: {}", correoPaciente, e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar la copia de la historia clínica por correo. Intenta de nuevo más tarde.");
        }
    }

    // NO @Async: el admin dispara el envio desde un boton y espera confirmacion
    // inmediata de si el correo salio o no, igual que enviarHistoriaClinica.
    @Override
    public void enviarReporteAdjunto(String correoDestino, String asunto, String descripcion,
                                     String nombreArchivo, byte[] contenido, String contentType) {
        if (correoDestino == null || correoDestino.isBlank()) {
            throw new RuntimeException("Debes indicar un correo de destino");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(correoDestino);
            helper.setSubject(asunto);
            String html = plantillaBase(asunto, "<p>" + descripcion + "</p>");
            helper.setText(html, true);
            helper.addAttachment(nombreArchivo,
                    new org.springframework.core.io.ByteArrayResource(contenido), contentType);
            mailSender.send(message);
            log.info("GMAIL: Reporte '{}' enviado a {}", nombreArchivo, correoDestino);
        } catch (Exception e) {
            log.error("GMAIL: Error enviando reporte a {}: {}", correoDestino, e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el reporte por correo. Intenta de nuevo más tarde.");
        }
    }

    // ===================== GMAIL SMTP =====================

    // Propaga el error (no lo traga) para que enviarRecordatorioCita() —el unico
    // llamador que depende del resultado real para decidir si marca el
    // recordatorio como enviado— se entere del fallo. Los demas llamadores
    // (notificaciones @Async de confirmacion/cancelacion/etc.) ya envuelven su
    // propia llamada en try/catch, asi que para ellos el comportamiento no cambia.
    private void enviar(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("GMAIL: Destinatario vacio, email no enviado");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            // "to" puede traer varios destinatarios separados por coma (p. ej. los
            // dos estudiantes de una misma cita): un solo correo para todos.
            helper.setTo(to.split("\\s*,\\s*"));
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("GMAIL: Email enviado a {} - {}", to, subject);
        } catch (MessagingException e) {
            log.error("GMAIL: Error enviando email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error enviando email a " + to, e);
        } catch (Exception e) {
            log.error("GMAIL: Error inesperado enviando email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error inesperado enviando email a " + to, e);
        }
    }

    // ===================== HTML TEMPLATES =====================

    private String plantillaBase(String titulo, String contenido) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif">
              <div style="max-width:560px;margin:32px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.1)">
                <div style="background:#1e40af;padding:24px 32px">
                  <h1 style="margin:0;color:#fff;font-size:20px">%s</h1>
                  <p style="margin:4px 0 0;color:#93c5fd;font-size:13px">Clinica de Optometria - Universidad El Bosque</p>
                </div>
                <div style="padding:24px 32px;color:#334155;font-size:14px;line-height:1.6">
                  %s
                </div>
                <div style="padding:16px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:center">
                  <p style="margin:0;color:#94a3b8;font-size:12px">NeoVision &bull; Sistema de Gestion de Citas</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(titulo, contenido);
    }

    private String detallesCita(String fecha, String horaInicio, String horaFin,
                                String salon, String espacio,
                                String nombreEstudiante, String nombreDocente,
                                String nombrePaciente, String especialidad) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='margin:16px 0;border-collapse:collapse'>");
        sb.append(fila("Fecha", fecha));
        sb.append(fila("Horario", horaInicio + " - " + horaFin));
        sb.append(fila("Lugar", espacio + " - " + salon));
        if (especialidad != null) sb.append(fila("Especialidad", especialidad));
        sb.append(fila("Estudiante", nombreEstudiante));
        if (nombreDocente != null) sb.append(fila("Docente", nombreDocente));
        sb.append(fila("Paciente", nombrePaciente));
        sb.append("</table>");
        return sb.toString();
    }

    private String bloqueRecordatorio(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return "<div style='margin:14px 0;padding:14px 16px;background:#fef3c7;border-left:4px solid #f59e0b;border-radius:6px'>"
                + "<p style='margin:0;color:#78350f;font-weight:600;font-size:14px'>📋 Antes de tu cita</p>"
                + "<p style='margin:6px 0 0;color:#78350f;font-size:13px'>" + texto + "</p>"
                + "</div>";
    }

    private String fila(String label, String value) {
        return "<tr><td style='padding:6px 16px 6px 0;color:#64748b;font-size:13px;white-space:nowrap'>"
                + label + "</td><td style='padding:6px 0;font-weight:600;font-size:14px'>"
                + value + "</td></tr>";
    }
}
