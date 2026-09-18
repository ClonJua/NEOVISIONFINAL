package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.dto.CrearSolicitudRequest;
import com.neovision.clinico.historia.dto.SolicitudResponse;
import com.neovision.clinico.historia.entity.SolicitudHistoria;
import com.neovision.clinico.historia.enums.CanalSolicitud;
import com.neovision.clinico.historia.enums.EstadoSolicitud;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import com.neovision.clinico.historia.repository.SolicitudHistoriaRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import com.neovision.common.exception.SolicitudNoEncontradaException;
import com.neovision.common.exception.UsuarioNoRegistradoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flujo de solicitud de copia de historia clínica: el paciente la crea, el admin
 * la aprueba (se genera el PDF y se envía al correo registrado) o la rechaza.
 */
@Service
public class SolicitudHistoriaService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudHistoriaService.class);

    private final SolicitudHistoriaRepository solicitudRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final HistoriaPdfService pdfService;
    private final EmailService emailService;

    public SolicitudHistoriaService(SolicitudHistoriaRepository solicitudRepository,
                                    PacienteRepository pacienteRepository,
                                    UsuarioRepository usuarioRepository,
                                    HistoriaClinicaRepository historiaClinicaRepository,
                                    HistoriaPdfService pdfService,
                                    EmailService emailService) {
        this.solicitudRepository = solicitudRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    private Paciente pacienteActual(Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new UsuarioNoRegistradoException("Usuario no encontrado"));
        return pacienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
    }

    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest req, Authentication auth) {
        Paciente paciente = pacienteActual(auth);
        if (!historiaClinicaRepository.existsByPacienteIdPaciente(paciente.getIdPaciente())) {
            throw new EstadoInvalidoException(
                    "Aún no tienes una historia clínica registrada: se abre en tu primera atención con un estudiante.");
        }
        if (solicitudRepository.existsByPacienteIdPacienteAndEstado(paciente.getIdPaciente(), EstadoSolicitud.PENDIENTE)) {
            throw new EstadoInvalidoException("Ya tienes una solicitud pendiente de revisión");
        }
        SolicitudHistoria s = new SolicitudHistoria();
        s.setPaciente(paciente);
        s.setFechaSolicitud(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setCanal(CanalSolicitud.DIGITAL);
        s.setMotivo(req != null ? req.getMotivo() : null);
        solicitudRepository.save(s);
        return toResponse(s);
    }

    /**
     * Registra una solicitud presencial y devuelve el PDF para entregarlo en mano.
     *
     * <p>Es el canal de quien no tiene cuenta ni correo — típicamente un adulto
     * mayor que llega a la clínica con su documento. El paciente está presente y
     * la copia se le entrega en el acto, así que la solicitud nace ya resuelta;
     * lo que importa registrar es que <em>ejerció su derecho de acceso</em> y
     * <em>quién verificó su identidad</em>, porque aquí no lo hace la autenticación.
     *
     * <p>La traza de lectura de la historia (acción {@code CONSULTAR}) la deja
     * {@code HistoriaPdfService} al generar el PDF.
     */
    @Transactional
    public byte[] crearPresencial(Integer idPaciente, String motivo, Authentication auth) {
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        if (!historiaClinicaRepository.existsByPacienteIdPaciente(idPaciente)) {
            throw new EstadoInvalidoException(
                    "Este paciente aún no tiene historia clínica: se abre en su primera atención con un estudiante.");
        }

        byte[] pdf = pdfService.generarPorPaciente(idPaciente, auth);

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudHistoria s = new SolicitudHistoria();
        s.setPaciente(paciente);
        s.setFechaSolicitud(ahora);
        s.setCanal(CanalSolicitud.PRESENCIAL);
        s.setCreadaPorCorreo(auth.getName());
        s.setMotivo(motivo);
        s.setEstado(EstadoSolicitud.APROBADA);
        s.setResueltaPorCorreo(auth.getName());
        s.setFechaResolucion(ahora);
        solicitudRepository.save(s);

        return pdf;
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> misSolicitudes(Authentication auth) {
        Paciente paciente = pacienteActual(auth);
        return solicitudRepository.findByPacienteIdPacienteOrderByFechaSolicitudDesc(paciente.getIdPaciente())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> listarTodas() {
        return solicitudRepository.findAllByOrderByFechaSolicitudDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SolicitudResponse aprobar(Integer idSolicitud, Authentication auth) {
        SolicitudHistoria s = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new EstadoInvalidoException("La solicitud ya fue resuelta");
        }
        Paciente paciente = s.getPaciente();

        byte[] pdf = pdfService.generarPorPaciente(paciente.getIdPaciente(), auth);
        if (paciente.getCorreo() != null && !paciente.getCorreo().isBlank()) {
            emailService.enviarHistoriaClinica(paciente.getCorreo(),
                    paciente.getNombre() + " " + paciente.getApellido(), pdf,
                    "historia-clinica-" + paciente.getIdPaciente() + ".pdf");
        } else {
            // Sin correo no hay a donde enviarla, pero la solicitud si queda resuelta:
            // la entrega se hace en mano descargando el PDF desde el listado.
            log.info("Solicitud {} aprobada sin envio: el paciente {} no tiene correo registrado; "
                    + "la copia debe entregarse presencialmente", s.getIdSolicitud(), paciente.getIdPaciente());
        }

        s.setEstado(EstadoSolicitud.APROBADA);
        s.setResueltaPorCorreo(auth.getName());
        s.setFechaResolucion(LocalDateTime.now());
        solicitudRepository.save(s);
        return toResponse(s);
    }

    @Transactional
    public SolicitudResponse rechazar(Integer idSolicitud, String motivo, Authentication auth) {
        SolicitudHistoria s = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new EstadoInvalidoException("La solicitud ya fue resuelta");
        }
        s.setEstado(EstadoSolicitud.RECHAZADA);
        s.setMotivoRechazo(motivo);
        s.setResueltaPorCorreo(auth.getName());
        s.setFechaResolucion(LocalDateTime.now());
        solicitudRepository.save(s);
        return toResponse(s);
    }

    private SolicitudResponse toResponse(SolicitudHistoria s) {
        Paciente p = s.getPaciente();
        return new SolicitudResponse(
                s.getIdSolicitud(),
                p != null ? p.getIdPaciente() : null,
                p != null ? p.getNombre() + " " + p.getApellido() : null,
                p != null ? p.getCorreo() : null,
                s.getFechaSolicitud(),
                s.getEstado().name(),
                s.getMotivo(),
                s.getMotivoRechazo(),
                s.getResueltaPorCorreo(),
                s.getFechaResolucion(),
                s.getCanal() != null ? s.getCanal().name() : null,
                s.getCreadaPorCorreo()
        );
    }
}
