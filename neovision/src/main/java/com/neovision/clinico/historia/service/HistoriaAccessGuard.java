package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.service.CitaEstudiantes;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.common.exception.AccesoNoAutorizadoHistoriaException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Control de acceso de LECTURA a la historia clínica por "necesidad de saber"
 * (minimum necessary, Ley 1581 / buenas prácticas hospitalarias). El RBAC de
 * path (SecurityConfig) solo decide el rol; aquí se exige además un vínculo
 * asistencial real con el paciente:
 *
 * <ul>
 *   <li><b>Admin/secretaría:</b> acceso administrativo a todas las historias.</li>
 *   <li><b>Docente:</b> solo pacientes atendidos en alguna de sus sesiones.</li>
 *   <li><b>Estudiante:</b> solo pacientes con los que tiene una cita asignada,
 *       y a nivel de atención individual, solo las suyas.</li>
 * </ul>
 *
 * Sin este vínculo se responde 403, evitando que cualquier usuario con rol
 * clínico consulte la historia de pacientes que no atiende.
 */
@Component
public class HistoriaAccessGuard {

    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final CitaRepository citaRepository;

    public HistoriaAccessGuard(EstudianteRepository estudianteRepository,
                               DocenteRepository docenteRepository,
                               CitaRepository citaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.citaRepository = citaRepository;
    }

    private static String rol(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(GrantedAuthority::getAuthority).orElse(null);
    }

    /** Autoriza la lectura de la historia (timeline) de un paciente. */
    public void requireLecturaPaciente(Integer idPaciente, Authentication auth) {
        String rol = rol(auth);
        if ("ROLE_ADMIN".equals(rol)) return;

        if ("ROLE_DOCENTE".equals(rol)) {
            Docente d = docenteRepository.findByCorreo(auth.getName())
                    .orElseThrow(() -> denied());
            if (citaRepository.existsBySesionDocenteIdDocenteAndPacienteIdPaciente(d.getIdDocente(), idPaciente)) return;
            throw denied();
        }
        if ("ROLE_ESTUDIANTE".equals(rol)) {
            Estudiante e = estudianteRepository.findByCorreo(auth.getName())
                    .orElseThrow(() -> denied());
            List<Cita> citas = citaRepository.findByEstudianteIdEstudianteAndPacienteIdPaciente(e.getIdEstudiante(), idPaciente);
            if (citas.isEmpty()) throw denied();
            if (citas.stream().anyMatch(HistoriaAccessGuard::dentroDeVentanaHoraria)) return;
            throw deniedFueraDeHorario();
        }
        throw denied();
    }

    /** Autoriza la lectura de una atención individual. */
    public void requireLecturaAtencion(Atencion a, Authentication auth) {
        String rol = rol(auth);
        if ("ROLE_ADMIN".equals(rol)) return;
        String correo = auth.getName();

        if ("ROLE_ESTUDIANTE".equals(rol)) {
            // El estudiante solo lee las atenciones que autoró o las de una cita
            // en la que es uno de los (hasta dos) estudiantes de la pareja, y
            // solo dentro de la ventana horaria de la cita (+/- 30 min de margen).
            boolean esAutor = a.getEstudiante() != null && correo.equalsIgnoreCase(a.getEstudiante().getCorreo());
            boolean esDeLaPareja = CitaEstudiantes.incluyeCorreo(a.getCita(), correo);
            if (!esAutor && !esDeLaPareja) throw denied();
            if (a.getCita() != null && !dentroDeVentanaHoraria(a.getCita())) throw deniedFueraDeHorario();
            return;
        }
        if ("ROLE_DOCENTE".equals(rol)) {
            // El docente lee las que supervisa, o las de pacientes que atiende en sus sesiones.
            if (a.getDocenteSupervisor() != null && correo.equalsIgnoreCase(a.getDocenteSupervisor().getCorreo())) return;
            Integer idPaciente = idPaciente(a);
            Docente d = docenteRepository.findByCorreo(correo).orElse(null);
            if (d != null && idPaciente != null
                    && citaRepository.existsBySesionDocenteIdDocenteAndPacienteIdPaciente(d.getIdDocente(), idPaciente)) return;
            throw denied();
        }
        throw denied();
    }

    private Integer idPaciente(Atencion a) {
        return a.getHistoriaClinica() != null && a.getHistoriaClinica().getPaciente() != null
                ? a.getHistoriaClinica().getPaciente().getIdPaciente() : null;
    }

    private AccesoNoAutorizadoHistoriaException denied() {
        return new AccesoNoAutorizadoHistoriaException(
                "No tienes acceso a la historia clínica de este paciente (no hay vínculo asistencial).");
    }

    private static final int VENTANA_MINUTOS = 30;

    /**
     * El estudiante solo puede leer la historia dentro del horario de la cita
     * asignada, con un margen de {@link #VENTANA_MINUTOS} minutos antes y
     * despues (ej. cita 08:00-12:00 -> visible de 07:30 a 12:30).
     */
    private static boolean dentroDeVentanaHoraria(Cita cita) {
        SesionPractica sesion = cita.getSesion();
        if (sesion == null || sesion.getFecha() == null) return false;
        LocalTime horaInicio = sesion.getHoraInicio() != null ? sesion.getHoraInicio() : LocalTime.MIN;
        LocalTime horaFin = sesion.getHoraFin() != null ? sesion.getHoraFin() : LocalTime.MAX;
        LocalDateTime desde = LocalDateTime.of(sesion.getFecha(), horaInicio).minusMinutes(VENTANA_MINUTOS);
        LocalDateTime hasta = LocalDateTime.of(sesion.getFecha(), horaFin).plusMinutes(VENTANA_MINUTOS);
        LocalDateTime ahora = LocalDateTime.now();
        return !ahora.isBefore(desde) && !ahora.isAfter(hasta);
    }

    private AccesoNoAutorizadoHistoriaException deniedFueraDeHorario() {
        return new AccesoNoAutorizadoHistoriaException(
                "Solo puedes ver la historia clínica del paciente dentro del horario de la cita (con 30 minutos de margen).");
    }
}
