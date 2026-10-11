package com.neovision.clinico.service;

import com.neovision.clinico.dto.CambiarEstadoCitaRequest;
import com.neovision.clinico.dto.CitaRequest;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.DistribucionEstudianteResponse;
import com.neovision.clinico.dto.EliminarVencidasResponse;
import com.neovision.clinico.dto.IndicadoresResponse;
import com.neovision.clinico.dto.ProgresoResponse;
import com.neovision.clinico.dto.EspecialidadResponse;
import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Especialidad;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.entity.Salon;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.entity.MetaPacientesSemestre;
import com.neovision.clinico.dto.MetaSemestreResponse;
import com.neovision.clinico.dto.CandidatoAvanceSemestreResponse;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.repository.EspecialidadRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.MetaPacientesSemestreRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.clinico.repository.SesionPracticaRepository;
import com.neovision.common.exception.AccesoNoAutorizadoException;
import com.neovision.common.exception.CitaNoEncontradaException;
import com.neovision.common.exception.EspecialidadNoEncontradaException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.EstudianteNoEncontradoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import com.neovision.common.exception.SesionNoEncontradaException;
import com.neovision.common.exception.UsuarioNoRegistradoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final SesionPracticaRepository sesionRepository;
    private final EstudianteRepository estudianteRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final EspecialidadRepository especialidadRepository;
    private final AtencionRepository atencionRepository;
    private final MetaPacientesSemestreRepository metaPacientesSemestreRepository;

    /** Veces que el paciente puede reprogramar una misma reserva. */
    static final int MAX_REPROGRAMACIONES_PACIENTE = 2;

    private static final java.util.Set<String> SUBTIPOS_CONTROL_VALIDOS = java.util.Set.of(
            "lentes de contacto", "funcional", "protesis", "prueba ambulatoria", "terapia"
    );

    public CitaServiceImpl(CitaRepository citaRepository,
                           SesionPracticaRepository sesionRepository,
                           EstudianteRepository estudianteRepository,
                           PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           EmailService emailService,
                           EspecialidadRepository especialidadRepository,
                           AtencionRepository atencionRepository,
                           MetaPacientesSemestreRepository metaPacientesSemestreRepository) {
        this.citaRepository = citaRepository;
        this.sesionRepository = sesionRepository;
        this.estudianteRepository = estudianteRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.especialidadRepository = especialidadRepository;
        this.atencionRepository = atencionRepository;
        this.metaPacientesSemestreRepository = metaPacientesSemestreRepository;
    }

    // ===================== ADMIN / SECRETARIA =====================

    @Override
    @Transactional
    public CitaResponse crearBloque(CitaRequest request) {
        SesionPractica sesion = sesionRepository.findById(request.getIdSesion())
                .orElseThrow(() -> new SesionNoEncontradaException("Sesión no encontrada"));

        // No permitir citas en sesiones de fechas pasadas
        if (sesion.getFecha().isBefore(LocalDate.now())) {
            throw new EstadoInvalidoException("No se pueden crear citas en fechas pasadas");
        }

        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        Estudiante estudiante2 = resolverEstudiante2(request.getIdEstudiante(), request.getIdEstudiante2());

        // Validar cupos con COUNT (no carga entidades)
        long cuposUsados = citaRepository.countActivasBySesion(sesion.getIdSesion());
        if (cuposUsados >= sesion.getCuposTotales()) {
            throw new EstadoInvalidoException("La sesión ya alcanzó el máximo de cupos (" + sesion.getCuposTotales() + ")");
        }

        // Validar que ninguno de los estudiantes del bloque tenga conflicto de horario
        requireSinConflictoHorario(estudiante, sesion);
        if (estudiante2 != null) requireSinConflictoHorario(estudiante2, sesion);

        Cita cita = new Cita();
        cita.setSesion(sesion);
        cita.setEstudiante(estudiante);
        cita.setEstudiante2(estudiante2);
        cita.setObservaciones(request.getObservaciones());

        if (request.getIdPaciente() != null) {
            Paciente paciente = pacienteRepository.findById(request.getIdPaciente())
                    .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
            requireSinCitaReservada(paciente.getIdPaciente(), null,
                    "Este paciente ya tiene una cita reservada. Debe esperar a que se complete, cancele o venza para agendar otra");
            cita.setPaciente(paciente);
            cita.setEstado("RESERVADO");
            cita.setFechaReserva(LocalDateTime.now());
        } else {
            cita.setEstado("DISPONIBLE");
        }

        citaRepository.save(cita);

        // Email de confirmacion si se asigno paciente
        if (cita.getPaciente() != null) {
            Docente docente = sesion.getDocente();
            Salon salonEmail = sesion.getSalon();
            String horaIniStr = sesion.getHoraInicio() != null ? sesion.getHoraInicio().toString() : "";
            String horaFinStr = sesion.getHoraFin() != null ? sesion.getHoraFin().toString() : "";
            String espNombre = cita.getEspecialidad() != null ? cita.getEspecialidad().getNombre() : null;
            String espObs = cita.getEspecialidad() != null
                    ? construirObservacionEmail(cita.getEspecialidad(), cita.getSubtipoControl())
                    : null;
            emailService.enviarConfirmacionReserva(
                    cita.getPaciente().getCorreo(), cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido(),
                    CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                    docente.getCorreo(), docente.getNombre() + " " + docente.getApellido(),
                    sesion.getFecha().toString(), horaIniStr, horaFinStr,
                    salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                    salonEmail != null ? salonEmail.getEspacio().getNombre() : "",
                    espNombre, espObs);
        }

        return toResponse(cita);
    }

    @Override
    @Transactional
    public CitaResponse editarCita(Integer idCita, CitaRequest request) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));

        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        Estudiante estudiante2 = resolverEstudiante2(request.getIdEstudiante(), request.getIdEstudiante2());

        cita.setEstudiante(estudiante);
        cita.setEstudiante2(estudiante2);
        cita.setObservaciones(request.getObservaciones());

        if (request.getIdPaciente() != null) {
            Paciente paciente = pacienteRepository.findById(request.getIdPaciente())
                    .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
            requireSinCitaReservada(paciente.getIdPaciente(), idCita,
                    "Este paciente ya tiene otra cita reservada");
            cita.setPaciente(paciente);
            if ("DISPONIBLE".equals(cita.getEstado())) {
                cita.setEstado("RESERVADO");
                cita.setFechaReserva(LocalDateTime.now());
            }
        } else if (cita.getPaciente() != null) {
            // El admin quito el paciente del formulario: liberar el bloque, salvo
            // que ya exista una atencion de por medio (quedaria la historia
            // clinica huerfana de la cita que la origino).
            if (atencionRepository.existsByCitaIdCita(idCita)) {
                throw new EstadoInvalidoException(
                        "No se puede quitar el paciente: esta cita ya tiene una atención de historia clínica asociada.");
            }
            cita.setPaciente(null);
            cita.setFechaReserva(null);
            if ("RESERVADO".equals(cita.getEstado())) {
                cita.setEstado("DISPONIBLE");
            }
        }

        citaRepository.save(cita);
        return toResponse(cita);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitas(String estado) {
        List<Cita> citas = estado != null
                ? citaRepository.findByEstado(estado)
                : citaRepository.findAll();
        return citas.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CitaResponse> listarCitas(String estado, Pageable pageable) {
        Page<Cita> citas = estado != null
                ? citaRepository.findByEstado(estado, pageable)
                : citaRepository.findAll(pageable);
        return citas.map(this::toResponse);
    }

    @Override
    @Transactional
    public void eliminarCita(Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));
        citaRepository.delete(cita);
    }

    // Limpieza masiva de bloques vencidos (nunca atendidos, ya sin ninguna acción posible).
    // Se omite (no se elimina) cualquier vencida que ya tenga una atención/historia clínica
    // asociada, para no arriesgar un registro clínico por una eliminación en lote.
    @Override
    @Transactional
    public EliminarVencidasResponse eliminarVencidas() {
        // No confiar solo en el scheduler (corre cada 5 min): un bloque que vencio
        // hace un instante todavia podria estar en RESERVADO/DISPONIBLE si el admin
        // presiona el boton antes del proximo tick, y el usuario veria "0 eliminadas"
        // aunque si haya bloques vencidos sin limpiar.
        citaRepository.marcarVencidas(java.time.LocalDate.now(), java.time.LocalTime.now());

        List<Cita> vencidas = citaRepository.findByEstado("VENCIDA");
        int eliminadas = 0;
        int omitidas = 0;
        for (Cita c : vencidas) {
            if (atencionRepository.existsByCitaIdCita(c.getIdCita())) {
                omitidas++;
                continue;
            }
            citaRepository.delete(c);
            eliminadas++;
        }
        return new EliminarVencidasResponse(eliminadas, omitidas);
    }

    @Override
    @Transactional
    public CitaResponse cambiarEstado(Integer idCita, CambiarEstadoCitaRequest request) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));

        String nuevoEstado = request.getEstado().toUpperCase();
        if (!List.of("COMPLETADO", "CANCELADO").contains(nuevoEstado)) {
            throw new RuntimeException("Estado inválido. Usa COMPLETADO o CANCELADO");
        }

        if ("COMPLETADO".equals(nuevoEstado) && !"RESERVADO".equals(cita.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden completar citas reservadas (con paciente asignado)");
        }

        if ("CANCELADO".equals(nuevoEstado)) {
            if (request.getObservaciones() != null) {
                cita.setObservaciones(request.getObservaciones());
            }
            // Si estaba reservada, liberar el bloque para que otro lo tome
            if ("RESERVADO".equals(cita.getEstado())) {
                // Guardar datos antes de limpiar
                Paciente paciente = cita.getPaciente();
                SesionPractica sesion = cita.getSesion();
                Estudiante estudiante = cita.getEstudiante();
                Docente docente = sesion.getDocente();

                cancelarYLiberarCupo(cita, "ADMIN", cita.getObservaciones());

                // Email de cancelacion por admin
                if (paciente != null) {
                    emailService.enviarCancelacion(
                            paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                            estudiante.getCorreo(), estudiante.getNombre() + " " + estudiante.getApellido(),
                            docente.getCorreo(), docente.getNombre() + " " + docente.getApellido(),
                            sesion.getFecha().toString(), horaIniStr(sesion), horaFinStr(sesion),
                            "Administracion", cita.getObservaciones());
                }
                return toResponse(cita);
            }
        }

        if ("COMPLETADO".equals(nuevoEstado) && cita.getPaciente() != null) {
            SesionPractica sesion = cita.getSesion();
            Paciente paciente = cita.getPaciente();
            Estudiante estudiante = cita.getEstudiante();
            emailService.enviarCompletada(
                    paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                    estudiante.getCorreo(), estudiante.getNombre() + " " + estudiante.getApellido(),
                    sesion.getFecha().toString(), horaIniStr(sesion), horaFinStr(sesion));
        }

        cita.setEstado(nuevoEstado);
        citaRepository.save(cita);
        return toResponse(cita);
    }

    // ===================== PACIENTE =====================

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> bloquesDisponibles() {
        // Filtro en BD en lugar de en memoria. Ademas usa @EntityGraph para evitar N+1.
        return citaRepository.findBloquesDisponibles(LocalDate.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> bloquesDisponiblesPorEspecialidad(Integer idEspecialidad) {
        if (idEspecialidad == null) {
            return bloquesDisponibles();
        }
        // Valida que la especialidad exista (evita query con FK invalido).
        especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
        return citaRepository.findBloquesDisponiblesPorEspecialidad(LocalDate.now(), idEspecialidad).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CitaResponse reservarBloque(String correoPaciente, Integer idCita,
                                        Integer idEspecialidad, String subtipoControl) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Bloque no encontrado"));

        if (!"DISPONIBLE".equals(cita.getEstado())) {
            throw new EstadoInvalidoException("Este bloque ya no está disponible");
        }

        Paciente paciente = findPacienteByCorreo(correoPaciente);

        requireSinCitaReservada(paciente.getIdPaciente(), null,
                "Ya tienes una cita reservada. Debes esperar a que se complete, se cancele o venza para agendar otra");

        // Resolver especialidad: si no viene una, usar la default (Funcional).
        Especialidad especialidad;
        if (idEspecialidad == null) {
            especialidad = especialidadRepository.findByEsDefaultTrue().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "No hay especialidad por defecto configurada. Contacta al administrador."));
        } else {
            especialidad = especialidadRepository.findById(idEspecialidad)
                    .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
            if (!Boolean.TRUE.equals(especialidad.getActivo())) {
                throw new EstadoInvalidoException("La especialidad seleccionada no está activa");
            }
        }

        // Validar que el docente de la sesion pueda atender esa especialidad
        // (excepto si es default — todos los docentes pueden hacerla).
        Docente docente = cita.getSesion().getDocente();
        boolean docentePuedeAtender = Boolean.TRUE.equals(especialidad.getEsDefault())
                || (docente.getEspecialidades() != null
                    && docente.getEspecialidades().stream()
                        .anyMatch(e -> e.getIdEspecialidad().equals(especialidad.getIdEspecialidad())));
        if (!docentePuedeAtender) {
            throw new EstadoInvalidoException(
                    "El docente que supervisa esta sesión no atiende la especialidad '"
                    + especialidad.getNombre() + "'. Selecciona otro horario.");
        }

        // Validar subtipo si la especialidad lo requiere (CONTROL).
        if (Boolean.TRUE.equals(especialidad.getRequiereSubtipo())) {
            if (subtipoControl == null || subtipoControl.isBlank()) {
                throw new RuntimeException(
                        "Esta especialidad requiere indicar el subtipo (lentes de contacto, funcional, protesis, prueba ambulatoria, terapia).");
            }
            String norm = subtipoControl.trim().toLowerCase();
            if (!SUBTIPOS_CONTROL_VALIDOS.contains(norm)) {
                throw new RuntimeException("Subtipo no válido: " + subtipoControl);
            }
            cita.setSubtipoControl(norm);
        } else {
            cita.setSubtipoControl(null);
        }

        cita.setEspecialidad(especialidad);
        cita.setPaciente(paciente);
        cita.setEstado("RESERVADO");
        cita.setFechaReserva(LocalDateTime.now());
        citaRepository.save(cita);

        // Email de confirmacion a todos los actores
        SesionPractica sesion = cita.getSesion();
        Salon salonEmail = sesion.getSalon();
        String observacionEmail = construirObservacionEmail(especialidad, cita.getSubtipoControl());
        emailService.enviarConfirmacionReserva(
                paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                docente.getCorreo(), docente.getNombre() + " " + docente.getApellido(),
                sesion.getFecha().toString(), horaIniStr(sesion), horaFinStr(sesion),
                salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                salonEmail != null ? salonEmail.getEspacio().getNombre() : "",
                especialidad.getNombre(), observacionEmail);

        return toResponse(cita);
    }

    private static String construirObservacionEmail(Especialidad e, String subtipo) {
        StringBuilder sb = new StringBuilder();
        if (e.getObservacionRecordatorio() != null && !e.getObservacionRecordatorio().isBlank()) {
            sb.append(e.getObservacionRecordatorio());
        }
        if (subtipo != null && !subtipo.isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append("Tipo de control: ").append(subtipo);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Override
    @Transactional
    public CitaResponse cancelarPaciente(String correoPaciente, Integer idCita, String observaciones) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));

        Paciente paciente = findPacienteByCorreo(correoPaciente);

        if (cita.getPaciente() == null || !cita.getPaciente().getIdPaciente().equals(paciente.getIdPaciente())) {
            throw new AccesoNoAutorizadoException("Esta cita no te pertenece");
        }

        if (!"RESERVADO".equals(cita.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden cancelar citas reservadas");
        }

        requireMotivoValido(observaciones, false);
        SesionPractica sesion = cita.getSesion();
        requireAntelacion(sesion, "cancelar");

        // Validar limite de cancelaciones: máximo 3 por mes
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        long cancelacionesEnMes = citaRepository.countCancelacionesEnMes(paciente.getIdPaciente(), hace30Dias);
        if (cancelacionesEnMes >= 3) {
            throw new EstadoInvalidoException("Has alcanzado el límite de 3 cancelaciones en los últimos 30 días. "
                    + "Si no puedes asistir, reprograma la cita o comunícate con la secretaría");
        }

        // Guardar datos del paciente antes de limpiar
        String correoPacienteNotif = paciente.getCorreo();
        String nombrePacienteNotif = paciente.getNombre() + " " + paciente.getApellido();
        String correosEstudiantes = CitaEstudiantes.correos(cita);
        String nombresEstudiantes = CitaEstudiantes.nombres(cita);
        Docente docente = sesion.getDocente();
        String fecha = sesion.getFecha().toString();
        String horaI = horaIniStr(sesion);
        String horaF = horaFinStr(sesion);

        cancelarYLiberarCupo(cita, "PACIENTE", observaciones);

        // Email de cancelacion
        emailService.enviarCancelacion(
                correoPacienteNotif, nombrePacienteNotif,
                correosEstudiantes, nombresEstudiantes,
                docente.getCorreo(), docente.getNombre() + " " + docente.getApellido(),
                fecha, horaI, horaF, "Paciente", observaciones);

        return toResponse(cita);
    }

    @Override
    @Transactional
    public CitaResponse reprogramarPaciente(String correoPaciente, Integer idCitaActual, Integer idCitaNueva) {
        Cita actual = citaRepository.findById(idCitaActual)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));
        Paciente paciente = findPacienteByCorreo(correoPaciente);

        if (actual.getPaciente() == null || !actual.getPaciente().getIdPaciente().equals(paciente.getIdPaciente())) {
            throw new AccesoNoAutorizadoException("Esta cita no te pertenece");
        }
        Cita nueva = citaRepository.findById(idCitaNueva)
                .orElseThrow(() -> new CitaNoEncontradaException("Bloque destino no encontrado"));
        requireAntelacion(actual.getSesion(), "reprogramar");
        if (actual.getReprogramacionesPaciente() >= MAX_REPROGRAMACIONES_PACIENTE) {
            throw new EstadoInvalidoException("Ya reprogramaste esta cita " + MAX_REPROGRAMACIONES_PACIENTE
                    + " veces. Si necesitas otro cambio, comunícate con la secretaría");
        }

        moverReserva(actual, nueva, "Reprogramada por el paciente", true);
        return toResponse(nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> misCitas(String correoPaciente) {
        Paciente paciente = findPacienteByCorreo(correoPaciente);
        return citaRepository.findByPacienteIdPaciente(paciente.getIdPaciente()).stream()
                .map(this::toResponse)
                .toList();
    }

    // ===================== ESTUDIANTE =====================

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> misBloques(String correoEstudiante) {
        Estudiante estudiante = findEstudianteByCorreo(correoEstudiante);
        return citaRepository.findByEstudianteIdEstudiante(estudiante.getIdEstudiante()).stream()
                .map(this::toResponse)
                .toList();
    }

    /*Se hace  la asignacion de paciente por atender
    @return Pa<ciente */
    @Override
    @Transactional(readOnly = true)
    public ProgresoResponse progresoEstudiante(String correoEstudiante) {
        Estudiante estudiante = findEstudianteByCorreo(correoEstudiante);
        int semestre = estudiante.getSemestre();
        int meta = metaPacientesSemestreRepository.findById(semestre)
                .map(MetaPacientesSemestre::getMinimoPacientes)
                .orElse(35);
        int atendidos = (int) citaRepository.countCompletadasByEstudiante(estudiante.getIdEstudiante());
        return new ProgresoResponse(semestre, atendidos, meta);
    }

    // ===================== ADMIN: METAS DE PACIENTES POR SEMESTRE =====================

    @Override
    @Transactional(readOnly = true)
    public List<MetaSemestreResponse> listarMetasSemestre() {
        return metaPacientesSemestreRepository.findAll().stream()
                .sorted(Comparator.comparing(MetaPacientesSemestre::getSemestre))
                .map(m -> new MetaSemestreResponse(m.getSemestre(), m.getMinimoPacientes()))
                .toList();
    }

    @Override
    @Transactional
    public MetaSemestreResponse actualizarMetaSemestre(Integer semestre, Integer minimoPacientes) {
        MetaPacientesSemestre meta = metaPacientesSemestreRepository.findById(semestre)
                .orElse(new MetaPacientesSemestre(semestre, minimoPacientes));
        meta.setMinimoPacientes(minimoPacientes);
        metaPacientesSemestreRepository.save(meta);
        return new MetaSemestreResponse(meta.getSemestre(), meta.getMinimoPacientes());
    }

    // ===================== ADMIN: AVANCE DE SEMESTRE EN LOTE =====================
    // 9 es terminal (no existe semestre 10 en este sistema): solo 7 y 8 pueden
    // subir automaticamente. Antes la secretaria tenia que editar el semestre de
    // cada estudiante uno por uno; ahora ve de una vez quien ya cumplio su meta
    // y los avanza con una sola accion, sin tocarlos individualmente.

    private static final List<Integer> SEMESTRES_AVANZABLES = List.of(7, 8);

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoAvanceSemestreResponse> listarCandidatosAvanceSemestre() {
        return estudianteRepository.findActivosPorSemestre(SEMESTRES_AVANZABLES).stream()
                .map(e -> {
                    int meta = metaPacientesSemestreRepository.findById(e.getSemestre())
                            .map(MetaPacientesSemestre::getMinimoPacientes)
                            .orElse(35);
                    int atendidos = (int) citaRepository.countCompletadasByEstudiante(e.getIdEstudiante());
                    return atendidos >= meta
                            ? new CandidatoAvanceSemestreResponse(
                                    e.getIdEstudiante(), e.getNombre() + " " + e.getApellido(),
                                    e.getSemestre(), atendidos, meta)
                            : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public int avanzarSemestre(List<Integer> idsEstudiantes) {
        if (idsEstudiantes == null || idsEstudiantes.isEmpty()) return 0;

        int avanzados = 0;
        for (Estudiante e : estudianteRepository.findAllById(idsEstudiantes)) {
            if (!SEMESTRES_AVANZABLES.contains(e.getSemestre())) continue;

            int meta = metaPacientesSemestreRepository.findById(e.getSemestre())
                    .map(MetaPacientesSemestre::getMinimoPacientes)
                    .orElse(35);
            int atendidos = (int) citaRepository.countCompletadasByEstudiante(e.getIdEstudiante());
            if (atendidos < meta) continue;

            e.setSemestre(e.getSemestre() + 1);
            estudianteRepository.save(e);
            avanzados++;
        }
        return avanzados;
    }

    @Override
    @Transactional
    public CitaResponse asignarPaciente(String correoEstudiante, Integer idCita, Integer idPaciente) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Bloque no encontrado"));

        Estudiante estudiante = findEstudianteByCorreo(correoEstudiante);

        // Cualquiera de los (hasta dos) estudiantes del bloque puede asignar el paciente.
        if (!CitaEstudiantes.incluye(cita, estudiante.getIdEstudiante())) {
            throw new AccesoNoAutorizadoException("Este bloque no te pertenece");
        }

        if (!"DISPONIBLE".equals(cita.getEstado())) {
            throw new EstadoInvalidoException("Este bloque ya no está disponible");
        }

        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));

        requireSinCitaReservada(paciente.getIdPaciente(), null,
                "Este paciente ya tiene una cita reservada. Debe esperar a que se complete, cancele o venza para agendar otra");

        cita.setPaciente(paciente);
        cita.setEstado("RESERVADO");
        cita.setFechaReserva(LocalDateTime.now());
        citaRepository.save(cita);

        // Email al paciente y a los estudiantes del bloque
        SesionPractica sesion = cita.getSesion();
        Salon salonEmail = sesion.getSalon();
        emailService.enviarAsignacionPaciente(
                paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                sesion.getFecha().toString(), horaIniStr(sesion), horaFinStr(sesion),
                salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                salonEmail != null ? salonEmail.getEspacio().getNombre() : "");

        return toResponse(cita);
    }

    /**
     * Confirma la asistencia (marca COMPLETADO) de un bloque, validando que la
     * cita pertenezca a una sesion asignada al docente autenticado. Antes esta
     * operacion la ejecutaba el propio estudiante; se cambio para que sea el
     * docente responsable quien confirme, ya que es quien supervisa la
     * atencion real del paciente.
     */
    @Override
    @Transactional
    public CitaResponse confirmarAsistenciaDocente(Integer idDocente, Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));

        if (!cita.getSesion().getDocente().getIdDocente().equals(idDocente)) {
            throw new AccesoNoAutorizadoException("Esta cita no pertenece a una sesión asignada a este docente");
        }

        CambiarEstadoCitaRequest req = new CambiarEstadoCitaRequest();
        req.setEstado("COMPLETADO");
        return cambiarEstado(idCita, req);
    }

    @Override
    @Transactional
    public CitaResponse cancelarEstudiante(String correoEstudiante, Integer idCita, String observaciones) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));
        Estudiante estudiante = findEstudianteByCorreo(correoEstudiante);

        if (!CitaEstudiantes.incluye(cita, estudiante.getIdEstudiante())) {
            throw new AccesoNoAutorizadoException("Este bloque no te pertenece");
        }
        if (!"RESERVADO".equals(cita.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden cancelar citas reservadas");
        }
        requireMotivoValido(observaciones, true);
        SesionPractica sesion = cita.getSesion();
        requireAntelacion(sesion, "cancelar");

        Paciente paciente = cita.getPaciente();
        Docente docente = sesion.getDocente();
        cancelarYLiberarCupo(cita, "ESTUDIANTE", observaciones);

        emailService.enviarCancelacion(
                paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                CitaEstudiantes.correos(cita), CitaEstudiantes.nombres(cita),
                docente.getCorreo(), docente.getNombre() + " " + docente.getApellido(),
                sesion.getFecha().toString(), horaIniStr(sesion), horaFinStr(sesion),
                "Estudiante", observaciones);

        return toResponse(cita);
    }

    @Override
    @Transactional
    public CitaResponse reprogramarEstudiante(String correoEstudiante, Integer idCitaActual, Integer idCitaNueva) {
        Cita actual = citaRepository.findById(idCitaActual)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada"));
        Cita nueva = citaRepository.findById(idCitaNueva)
                .orElseThrow(() -> new CitaNoEncontradaException("Bloque destino no encontrado"));
        Estudiante estudiante = findEstudianteByCorreo(correoEstudiante);

        // El estudiante solo mueve pacientes entre sus propios bloques.
        if (!CitaEstudiantes.incluye(actual, estudiante.getIdEstudiante())
                || !CitaEstudiantes.incluye(nueva, estudiante.getIdEstudiante())) {
            throw new AccesoNoAutorizadoException("Solo puedes reprogramar entre tus propios bloques");
        }
        requireAntelacion(actual.getSesion(), "reprogramar");

        moverReserva(actual, nueva, "Reprogramada por el estudiante", false);
        return toResponse(nueva);
    }

    // ===================== ADMIN REPROGRAMAR =====================

    @Override
    @Transactional
    public CitaResponse reprogramarAdmin(Integer idCitaActual, Integer idCitaNueva) {
        Cita citaActual = citaRepository.findById(idCitaActual)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita actual no encontrada"));
        Cita citaNueva = citaRepository.findById(idCitaNueva)
                .orElseThrow(() -> new CitaNoEncontradaException("Bloque destino no encontrado"));

        moverReserva(citaActual, citaNueva, "Reprogramada por administracion", false);
        return toResponse(citaNueva);
    }

    // ===================== HELPERS =====================

    /**
     * Marca la cita como CANCELADO conservando al paciente (historial y limite de
     * cancelaciones) y libera el cupo creando una cita DISPONIBLE nueva en el
     * mismo bloque, con los mismos estudiantes.
     */
    private void cancelarYLiberarCupo(Cita cita, String canceladaPor, String observaciones) {
        cita.setEstado("CANCELADO");
        cita.setCanceladaPor(canceladaPor);
        cita.setFechaCancelacion(LocalDateTime.now());
        cita.setObservaciones(observaciones);
        citaRepository.save(cita);

        Cita libre = new Cita();
        libre.setSesion(cita.getSesion());
        libre.setEstudiante(cita.getEstudiante());
        libre.setEstudiante2(cita.getEstudiante2());
        libre.setEstado("DISPONIBLE");
        citaRepository.save(libre);
    }

    /**
     * Mueve la reserva de un paciente de una cita RESERVADO a un bloque DISPONIBLE,
     * conservando especialidad, subtipo y el contador de reprogramaciones (que sube
     * solo si la pide el paciente), y libera el bloque original. Envia el correo
     * de reprogramacion a todos los actores.
     */
    private void moverReserva(Cita actual, Cita nueva, String motivo, boolean porPaciente) {
        if (!"RESERVADO".equals(actual.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden reprogramar citas reservadas");
        }
        if (!"DISPONIBLE".equals(nueva.getEstado())) {
            throw new EstadoInvalidoException("El bloque destino ya no está disponible");
        }
        if (actual.getIdCita().equals(nueva.getIdCita())) {
            throw new EstadoInvalidoException("El bloque destino debe ser distinto del actual");
        }
        if (!Boolean.TRUE.equals(nueva.getSesion().getActivo())) {
            throw new EstadoInvalidoException("La sesión del bloque destino no está activa");
        }
        requireSesionFutura(nueva.getSesion());
        // Si ya se abrio la historia clinica de esta cita, moverla dejaria la
        // atencion colgada de un bloque libre sin paciente.
        if (atencionRepository.existsByCitaIdCita(actual.getIdCita())) {
            throw new EstadoInvalidoException(
                    "No se puede reprogramar: esta cita ya tiene una atención de historia clínica asociada.");
        }

        Paciente paciente = actual.getPaciente();
        Especialidad especialidad = actual.getEspecialidad();
        String subtipo = actual.getSubtipoControl();
        int reprogramaciones = actual.getReprogramacionesPaciente() + (porPaciente ? 1 : 0);

        // El paciente no puede tener otra cita reservada aparte de la que se mueve
        requireSinCitaReservada(paciente.getIdPaciente(), actual.getIdCita(),
                "El paciente ya tiene otra cita reservada");

        // El docente del bloque destino debe poder atender la especialidad de la cita.
        Docente docenteNuevo = nueva.getSesion().getDocente();
        if (especialidad != null && !Boolean.TRUE.equals(especialidad.getEsDefault())
                && (docenteNuevo.getEspecialidades() == null
                    || docenteNuevo.getEspecialidades().stream()
                        .noneMatch(e -> e.getIdEspecialidad().equals(especialidad.getIdEspecialidad())))) {
            throw new EstadoInvalidoException(
                    "El docente del bloque destino no atiende la especialidad '"
                    + especialidad.getNombre() + "'. Selecciona otro horario.");
        }

        SesionPractica sesionAnterior = actual.getSesion();

        // Liberar la cita actual
        actual.setEstado("DISPONIBLE");
        actual.setPaciente(null);
        actual.setFechaReserva(null);
        actual.setEspecialidad(null);
        actual.setSubtipoControl(null);
        actual.setRecordatorio24hEnviado(false);
        actual.setRecordatorio1hEnviado(false);
        actual.setReprogramacionesPaciente(0);
        actual.setObservaciones(motivo);
        citaRepository.save(actual);

        // Reservar la nueva
        nueva.setPaciente(paciente);
        nueva.setEspecialidad(especialidad);
        nueva.setSubtipoControl(subtipo);
        nueva.setEstado("RESERVADO");
        nueva.setFechaReserva(LocalDateTime.now());
        nueva.setRecordatorio24hEnviado(false);
        nueva.setRecordatorio1hEnviado(false);
        nueva.setReprogramacionesPaciente(reprogramaciones);
        citaRepository.save(nueva);

        SesionPractica sesionNueva = nueva.getSesion();
        Salon salonEmail = sesionNueva.getSalon();
        emailService.enviarReprogramacion(
                paciente.getCorreo(), paciente.getNombre() + " " + paciente.getApellido(),
                CitaEstudiantes.correos(nueva), CitaEstudiantes.nombres(nueva),
                docenteNuevo.getCorreo(), docenteNuevo.getNombre() + " " + docenteNuevo.getApellido(),
                sesionAnterior.getFecha().toString(), horaIniStr(sesionAnterior),
                sesionNueva.getFecha().toString(), horaIniStr(sesionNueva),
                salonEmail != null ? salonEmail.getNombre() : "Sin salon",
                salonEmail != null ? salonEmail.getEspacio().getNombre() : "");
    }

    /** La columna observaciones admite 255 caracteres; el motivo puede ser obligatorio. */
    private void requireMotivoValido(String motivo, boolean obligatorio) {
        if (obligatorio && (motivo == null || motivo.isBlank())) {
            throw new EstadoInvalidoException("Debes indicar el motivo de la cancelación");
        }
        if (motivo != null && motivo.length() > 255) {
            throw new EstadoInvalidoException("El motivo no puede superar 255 caracteres");
        }
    }

    /** Exige al menos 1 hora de antelacion (solo si la sesion tiene hora definida). */
    private void requireAntelacion(SesionPractica sesion, String accion) {
        if (sesion.getHoraInicio() == null) return;
        LocalDateTime fechaHoraCita = LocalDateTime.of(sesion.getFecha(), sesion.getHoraInicio());
        if (LocalDateTime.now().plusHours(1).isAfter(fechaHoraCita)) {
            throw new EstadoInvalidoException("Solo puedes " + accion + " con al menos 1 hora de antelación");
        }
    }

    /** El bloque destino de una reprogramacion no puede estar en el pasado. */
    private void requireSesionFutura(SesionPractica sesion) {
        LocalDateTime inicio = sesion.getHoraInicio() != null
                ? LocalDateTime.of(sesion.getFecha(), sesion.getHoraInicio())
                : sesion.getFecha().atStartOfDay().plusDays(1);
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new EstadoInvalidoException("El bloque destino ya pasó");
        }
    }

    /**
     * Exige que el paciente no tenga ya otra cita RESERVADO. Un paciente solo
     * puede tener una cita activa a la vez: como la cita ya agrupa a los (hasta
     * dos) estudiantes que lo atienden en pareja, nunca hace falta una segunda
     * reserva para el mismo paciente — y permitirla se veía como dos citas
     * distintas en su agenda.
     */
    private void requireSinCitaReservada(Integer idPaciente, Integer idCitaExcluir, String mensaje) {
        if (citaRepository.existeCitaReservada(idPaciente, "RESERVADO", idCitaExcluir)) {
            throw new EstadoInvalidoException(mensaje);
        }
    }

    /** Segundo estudiante del bloque (opcional). Debe existir y ser distinto del primero. */
    private Estudiante resolverEstudiante2(Integer idEstudiante, Integer idEstudiante2) {
        if (idEstudiante2 == null) return null;
        if (idEstudiante2.equals(idEstudiante)) {
            throw new EstadoInvalidoException("El segundo estudiante debe ser distinto del primero");
        }
        return estudianteRepository.findById(idEstudiante2)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Segundo estudiante no encontrado"));
    }

    /** Un estudiante no puede estar en dos bloques (de cualquier cita) en el mismo horario. */
    private void requireSinConflictoHorario(Estudiante estudiante, SesionPractica sesion) {
        if (sesion.getHoraInicio() == null || sesion.getHoraFin() == null) return;
        List<Cita> citasConflicto = citaRepository.findCitasConflicto(
                estudiante.getIdEstudiante(), sesion.getFecha(), sesion.getHoraInicio(), sesion.getHoraFin());
        if (!citasConflicto.isEmpty()) {
            throw new EstadoInvalidoException("El estudiante " + estudiante.getNombre() + " " + estudiante.getApellido()
                    + " ya tiene una cita asignada en este horario. No puede tener dos pacientes al mismo tiempo.");
        }
    }

    private Paciente findPacienteByCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoRegistradoException("Usuario no encontrado"));
        return pacienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
    }

    private Estudiante findEstudianteByCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoRegistradoException("Usuario no encontrado"));
        return estudianteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
    }

    private CitaResponse toResponse(Cita cita) {
        SesionPractica sesion = cita.getSesion();
        Estudiante estudiante = cita.getEstudiante();
        Estudiante estudiante2 = cita.getEstudiante2();
        Paciente paciente = cita.getPaciente();
        Salon salon = sesion.getSalon();
        Docente docente = sesion.getDocente();
        Especialidad esp = cita.getEspecialidad();

        java.util.List<EspecialidadResponse> especialidadesDocente = java.util.Collections.emptyList();
        if (docente.getEspecialidades() != null) {
            especialidadesDocente = docente.getEspecialidades().stream()
                    .map(e -> new EspecialidadResponse(
                            e.getIdEspecialidad(), e.getNombre(),
                            e.getObservacionRecordatorio(),
                            e.getRequiereSubtipo(), e.getEsDefault(), e.getActivo()))
                    .toList();
        }

        return new CitaResponse(
                cita.getIdCita(),
                cita.getEstado(),
                horaIniStr(sesion),
                horaFinStr(sesion),
                cita.getObservaciones(),
                cita.getFechaReserva() != null ? cita.getFechaReserva().toString() : null,
                sesion.getIdSesion(),
                sesion.getFecha().toString(),
                salon != null ? salon.getNombre() : null,
                salon != null ? salon.getEspacio().getNombre() : null,
                docente.getNombre() + " " + docente.getApellido(),
                estudiante.getIdEstudiante(),
                estudiante.getNombre() + " " + estudiante.getApellido(),
                estudiante2 != null ? estudiante2.getIdEstudiante() : null,
                estudiante2 != null ? estudiante2.getNombre() + " " + estudiante2.getApellido() : null,
                paciente != null ? paciente.getIdPaciente() : null,
                paciente != null ? paciente.getNombre() + " " + paciente.getApellido() : null,
                paciente != null ? paciente.getApellido() : null,
                paciente != null ? paciente.getTipoDocumento() : null,
                paciente != null ? paciente.getNumeroDocumento() : null,
                esp != null ? esp.getIdEspecialidad() : null,
                esp != null ? esp.getNombre() : null,
                esp != null ? esp.getObservacionRecordatorio() : null,
                cita.getSubtipoControl(),
                especialidadesDocente,
                cita.getCanceladaPor(),
                cita.getFechaCancelacion() != null ? cita.getFechaCancelacion().toString() : null,
                Math.max(0, MAX_REPROGRAMACIONES_PACIENTE - cita.getReprogramacionesPaciente())
        );
    }

    private static String horaIniStr(SesionPractica sesion) {
        return sesion.getHoraInicio() != null ? sesion.getHoraInicio().toString() : "";
    }

    private static String horaFinStr(SesionPractica sesion) {
        return sesion.getHoraFin() != null ? sesion.getHoraFin().toString() : "";
    }

    // ===================== ADMIN HISTORIAL =====================

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> historialPaciente(Integer idPaciente) {
        return citaRepository.findByPacienteIdPaciente(idPaciente).stream()
                .sorted((a, b) -> {
                    int fechaCmp = b.getSesion().getFecha().compareTo(a.getSesion().getFecha());
                    if (fechaCmp != 0) return fechaCmp;
                    if (a.getSesion().getHoraInicio() == null) return 1;
                    if (b.getSesion().getHoraInicio() == null) return -1;
                    return b.getSesion().getHoraInicio().compareTo(a.getSesion().getHoraInicio());
                })
                .map(this::toResponse)
                .toList();
    }

    // ===================== DOCENTE =====================

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> misCitasDocente(Integer idDocente) {
        return citaRepository.findByDocenteIdDocente(idDocente)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ===================== ADMIN REPORTES =====================
    // Agregados en BD (GROUP BY / COUNT DISTINCT) o filtrados a una ventana
    // acotada, en vez de cargar todas las citas del sistema a memoria del
    // navegador. Escala independientemente del volumen historico.

    @Override
    @Transactional(readOnly = true)
    public IndicadoresResponse indicadores() {
        Map<String, Long> porEstado = citaRepository.conteoPorEstado().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        long consultorios = citaRepository.countDistinctSalonesUsados();
        long estudiantesActivos = citaRepository.countDistinctEstudiantesConCita();
        return new IndicadoresResponse(porEstado, consultorios, estudiantesActivos, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistribucionEstudianteResponse> distribucionPorEstudiante() {
        // Una cita completada cuenta para los dos estudiantes del bloque: se suman
        // los conteos del primer y del segundo puesto.
        Map<Integer, Long> completadasPorEstudiante = citaRepository.conteoCompletadasPorEstudiante().stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
        citaRepository.conteoCompletadasPorEstudiante2()
                .forEach(row -> completadasPorEstudiante.merge((Integer) row[0], (Long) row[1], Long::sum));
        return estudianteRepository.findAll().stream()
                .filter(e -> e.getUsuario() != null && Boolean.TRUE.equals(e.getUsuario().getEstado()))
                .map(e -> new DistribucionEstudianteResponse(
                        e.getIdEstudiante(),
                        e.getNombre() + " " + e.getApellido(),
                        e.getSemestre(),
                        completadasPorEstudiante.getOrDefault(e.getIdEstudiante(), 0L)))
                .sorted(Comparator.comparing(DistribucionEstudianteResponse::getNombre))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> ocupacionReciente() {
        // Margen de 11 semanas para asegurar cubrir "las ultimas 10 semanas con
        // actividad" que agrupa el frontend, sin traer el historico completo.
        LocalDate desde = LocalDate.now().minusWeeks(11);
        return citaRepository.findBySesionFechaGreaterThanEqualOrderBySesionFechaAsc(desde).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> inasistencias() {
        return citaRepository.findByEstadoInOrderBySesionFechaDesc(List.of("CANCELADO", "VENCIDA")).stream()
                .map(this::toResponse)
                .toList();
    }
}
