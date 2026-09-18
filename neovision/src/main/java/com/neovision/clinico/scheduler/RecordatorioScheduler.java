package com.neovision.clinico.scheduler;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Salon;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.service.CitaEstudiantes;
import com.neovision.common.service.EmailService;
import com.neovision.common.service.ExpoPushService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final CitaRepository citaRepository;
    private final EmailService emailService;
    private final ExpoPushService expoPushService;

    public RecordatorioScheduler(CitaRepository citaRepository,
                                 EmailService emailService,
                                 ExpoPushService expoPushService) {
        this.citaRepository = citaRepository;
        this.emailService = emailService;
        this.expoPushService = expoPushService;
    }

    /** Envia push al paciente y estudiante de la cita, si tienen dispositivo registrado. */
    private void enviarPushRecordatorio(Cita cita, String cuando) {
        String fecha = cita.getSesion().getFecha().toString();
        String hora  = cita.getSesion().getHoraInicio() != null
                ? cita.getSesion().getHoraInicio().toString().substring(0, 5) : "";
        String cuerpo = "Tu cita es " + cuando + " (" + fecha + (hora.isEmpty() ? "" : " " + hora) + ").";

        if (cita.getPaciente() != null && cita.getPaciente().getUsuario() != null) {
            expoPushService.enviar(
                    cita.getPaciente().getUsuario().getExpoPushToken(),
                    "Recordatorio de cita", cuerpo, cita.getIdCita());
        }
        if (cita.getEstudiante() != null && cita.getEstudiante().getUsuario() != null) {
            expoPushService.enviar(
                    cita.getEstudiante().getUsuario().getExpoPushToken(),
                    "Recordatorio de cita", cuerpo, cita.getIdCita());
        }
        // La cita puede tener un segundo estudiante (atienden en pareja).
        if (cita.getEstudiante2() != null && cita.getEstudiante2().getUsuario() != null) {
            expoPushService.enviar(
                    cita.getEstudiante2().getUsuario().getExpoPushToken(),
                    "Recordatorio de cita", cuerpo, cita.getIdCita());
        }
    }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "enviarRecordatorios", lockAtMostFor = "55s", lockAtLeastFor = "30s")
    @Transactional
    public void enviarRecordatorios() {
        try {
            enviarRecordatorios24h();
            enviarRecordatorios1h();
        } catch (Exception e) {
            log.error("RECORDATORIO: Error en scheduler de recordatorios: {}", e.getMessage(), e);
        }
    }

    private void enviarRecordatorios24h() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Cita> citas = citaRepository.findCitasRecordatorio24h(manana);

        for (Cita cita : citas) {
            try {
                if (cita.getPaciente() != null && cita.getEstudiante() != null) {
                    Salon salonEmail = cita.getSesion().getSalon();
                    String horaIni = cita.getSesion().getHoraInicio() != null ? cita.getSesion().getHoraInicio().toString() : "";
                    String horaFin = cita.getSesion().getHoraFin() != null ? cita.getSesion().getHoraFin().toString() : "";

                    String especialidadNombre = cita.getEspecialidad() != null ? cita.getEspecialidad().getNombre() : null;
                    String observacion = construirObservacion(cita);

                    emailService.enviarRecordatorioCita(
                            cita.getPaciente().getCorreo(), cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido(),
                            CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                            cita.getSesion().getDocente().getCorreo(), cita.getSesion().getDocente().getNombre() + " " + cita.getSesion().getDocente().getApellido(),
                            cita.getSesion().getFecha().toString(), horaIni, horaFin,
                            salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                            salonEmail != null ? salonEmail.getEspacio().getNombre() : "",
                            "en 24 horas",
                            especialidadNombre, observacion);

                    enviarPushRecordatorio(cita, "en 24 horas");

                    cita.setRecordatorio24hEnviado(true);
                    citaRepository.save(cita);
                    log.info("RECORDATORIO: Email 24h enviado para cita #{}", cita.getIdCita());
                }
            } catch (Exception e) {
                log.error("RECORDATORIO: Error enviando recordatorio 24h para cita #{}: {}", cita.getIdCita(), e.getMessage(), e);
            }
        }
    }

    private static String construirObservacion(Cita cita) {
        if (cita.getEspecialidad() == null) return null;
        StringBuilder sb = new StringBuilder();
        String obs = cita.getEspecialidad().getObservacionRecordatorio();
        if (obs != null && !obs.isBlank()) sb.append(obs);
        if (cita.getSubtipoControl() != null && !cita.getSubtipoControl().isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append("Tipo de control: ").append(cita.getSubtipoControl());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void enviarRecordatorios1h() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        LocalTime ahora1h = ahora.plusHours(1);
        LocalTime ahoraMenos1h = ahora.minusHours(1);

        List<Cita> citas = citaRepository.findCitasRecordatorio1h(hoy, ahora1h, ahoraMenos1h);

        for (Cita cita : citas) {
            try {
                if (cita.getPaciente() != null && cita.getEstudiante() != null) {
                    Salon salonEmail = cita.getSesion().getSalon();
                    String horaIni = cita.getSesion().getHoraInicio() != null ? cita.getSesion().getHoraInicio().toString() : "";
                    String horaFin = cita.getSesion().getHoraFin() != null ? cita.getSesion().getHoraFin().toString() : "";

                    String especialidadNombre = cita.getEspecialidad() != null ? cita.getEspecialidad().getNombre() : null;
                    String observacion = construirObservacion(cita);

                    emailService.enviarRecordatorioCita(
                            cita.getPaciente().getCorreo(), cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido(),
                            CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                            cita.getSesion().getDocente().getCorreo(), cita.getSesion().getDocente().getNombre() + " " + cita.getSesion().getDocente().getApellido(),
                            cita.getSesion().getFecha().toString(), horaIni, horaFin,
                            salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                            salonEmail != null ? salonEmail.getEspacio().getNombre() : "",
                            "en 1 hora",
                            especialidadNombre, observacion);

                    enviarPushRecordatorio(cita, "en 1 hora");

                    cita.setRecordatorio1hEnviado(true);
                    citaRepository.save(cita);
                    log.info("RECORDATORIO: Email 1h enviado para cita #{}", cita.getIdCita());
                }
            } catch (Exception e) {
                log.error("RECORDATORIO: Error enviando recordatorio 1h para cita #{}: {}", cita.getIdCita(), e.getMessage(), e);
            }
        }
    }
}
