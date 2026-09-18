package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.dto.CrearSolicitudRequest;
import com.neovision.clinico.historia.dto.SolicitudResponse;
import com.neovision.clinico.historia.entity.SolicitudHistoria;
import com.neovision.clinico.historia.enums.EstadoSolicitud;
import com.neovision.clinico.historia.repository.SolicitudHistoriaRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import com.neovision.common.exception.SolicitudNoEncontradaException;
import com.neovision.common.exception.UsuarioNoRegistradoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
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

    private final SolicitudHistoriaRepository solicitudRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoriaPdfService pdfService;
    private final EmailService emailService;

    public SolicitudHistoriaService(SolicitudHistoriaRepository solicitudRepository,
                                    PacienteRepository pacienteRepository,
                                    UsuarioRepository usuarioRepository,
                                    HistoriaPdfService pdfService,
                                    EmailService emailService) {
        this.solicitudRepository = solicitudRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
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
        if (solicitudRepository.existsByPacienteIdPacienteAndEstado(paciente.getIdPaciente(), EstadoSolicitud.PENDIENTE)) {
            throw new EstadoInvalidoException("Ya tienes una solicitud pendiente de revisión");
        }
        SolicitudHistoria s = new SolicitudHistoria();
        s.setPaciente(paciente);
        s.setFechaSolicitud(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setMotivo(req != null ? req.getMotivo() : null);
        solicitudRepository.save(s);
        return toResponse(s);
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
        emailService.enviarHistoriaClinica(paciente.getCorreo(),
                paciente.getNombre() + " " + paciente.getApellido(), pdf,
                "historia-clinica-" + paciente.getIdPaciente() + ".pdf");

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
                s.getFechaResolucion()
        );
    }
}
