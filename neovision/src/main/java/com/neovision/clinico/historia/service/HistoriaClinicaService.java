package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.dto.AbrirHistoriaRequest;
import com.neovision.clinico.historia.dto.HistoriaClinicaResponse;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.entity.HistoriaClinica;
import com.neovision.clinico.historia.enums.AccionAuditoria;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Apertura y consulta de historias clínicas. La autorización de path la garantiza
 * SecurityConfig; aquí se aplican reglas de negocio y se registra auditoría.
 */
@Service
public class HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaRepository;
    private final AtencionRepository atencionRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditoriaService auditoriaService;
    private final HistoriaAccessGuard accessGuard;
    private final HistoriaMapper mapper;

    public HistoriaClinicaService(HistoriaClinicaRepository historiaRepository,
                                  AtencionRepository atencionRepository,
                                  PacienteRepository pacienteRepository,
                                  AuditoriaService auditoriaService,
                                  HistoriaAccessGuard accessGuard,
                                  HistoriaMapper mapper) {
        this.historiaRepository = historiaRepository;
        this.atencionRepository = atencionRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditoriaService = auditoriaService;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
    }

    static String rol(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(GrantedAuthority::getAuthority).orElse(null);
    }

    @Transactional
    public HistoriaClinicaResponse abrirHistoria(Integer idPaciente, AbrirHistoriaRequest req, Authentication auth) {
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));

        // Mismo vinculo asistencial que se exige para leer la historia: el
        // estudiante debe tener una cita con este paciente. Sin este chequeo,
        // cualquier estudiante podia abrir una historia para un paciente con el
        // que nunca ha tenido contacto.
        accessGuard.requireLecturaPaciente(idPaciente, auth);

        if (historiaRepository.existsByPacienteIdPaciente(idPaciente)) {
            throw new EstadoInvalidoException("El paciente ya tiene una historia clínica abierta");
        }

        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setNumeroHistoria(req.getNumeroHistoria());
        historia.setFechaApertura(LocalDateTime.now());
        historia.setAbiertaPorCorreo(auth.getName());
        historia.setEstado("ACTIVA");
        historiaRepository.save(historia);

        auditoriaService.registrar(auth.getName(), rol(auth), AccionAuditoria.CREAR_HISTORIA,
                "HISTORIA", historia.getIdHistoria(), idPaciente, "Apertura de historia clínica");

        // El consentimiento de tratamiento de datos se captura al crear la cuenta
        // del paciente (Ley 1581); aquí solo reflejamos su estado.
        boolean autorizo = Boolean.TRUE.equals(paciente.getAutorizaTratamientoDatos());
        return mapper.toHistoriaResponse(historia, paciente, List.of(), autorizo);
    }

    @Transactional
    public HistoriaClinicaResponse obtenerPorPaciente(Integer idPaciente, Authentication auth) {
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));

        // Necesidad de saber: solo si hay vínculo asistencial con el paciente.
        accessGuard.requireLecturaPaciente(idPaciente, auth);

        HistoriaClinica historia = historiaRepository.findByPacienteIdPaciente(idPaciente).orElse(null);
        if (historia == null) {
            // Sin historia aún: idHistoria null indica al frontend que debe abrirla.
            return mapper.toHistoriaResponse(null, paciente, List.of(), false);
        }

        List<Atencion> atenciones = atencionRepository
                .findByHistoriaClinicaIdHistoriaOrderByFechaAtencionDesc(historia.getIdHistoria());
        boolean tieneConsentimiento = Boolean.TRUE.equals(paciente.getAutorizaTratamientoDatos());

        auditoriaService.registrar(auth.getName(), rol(auth), AccionAuditoria.CONSULTAR,
                "HISTORIA", historia.getIdHistoria(), idPaciente, "Consulta de historia clínica");

        return mapper.toHistoriaResponse(historia, paciente, atenciones, tieneConsentimiento);
    }

    /**
     * Listado para el admin/secretaria (consulta), sin paginar. A diferencia de
     * "listar citas" (que crece indefinidamente con cada bloque agendado), el
     * volumen aquí está acotado por el número de pacientes con historia abierta
     * — crece mucho más lento y se mantiene en un rango manejable para cargarlo
     * completo. Si el programa llega a tener varios miles de pacientes, esto
     * debería paginarse igual que se hizo con /admin/citas/reportes.
     */
    @Transactional(readOnly = true)
    public List<HistoriaClinicaResponse> listarTodas() {
        return historiaRepository.findAll().stream()
                .map(h -> mapper.toHistoriaResponse(h, h.getPaciente(), List.of(), false))
                .toList();
    }
}
