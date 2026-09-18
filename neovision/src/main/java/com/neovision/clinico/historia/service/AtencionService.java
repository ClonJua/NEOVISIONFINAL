package com.neovision.clinico.historia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.dto.AdendaRequest;
import com.neovision.clinico.historia.dto.AdendaResponse;
import com.neovision.clinico.historia.dto.AtencionResponse;
import com.neovision.clinico.historia.dto.AtencionResumenResponse;
import com.neovision.clinico.historia.dto.DevolverAtencionRequest;
import com.neovision.clinico.historia.dto.DiagnosticoDto;
import com.neovision.clinico.historia.dto.EnviarRevisionRequest;
import com.neovision.clinico.historia.dto.FinalizarAtencionRequest;
import com.neovision.clinico.historia.dto.GuardarAtencionRequest;
import com.neovision.clinico.historia.dto.ProcedimientoDto;
import com.neovision.clinico.historia.dto.VerificacionIntegridadResponse;
import com.neovision.clinico.historia.embeddable.DatosGenerales;
import com.neovision.clinico.historia.entity.Adenda;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.entity.Diagnostico;
import com.neovision.clinico.historia.entity.HistoriaClinica;
import com.neovision.clinico.historia.entity.Procedimiento;
import com.neovision.clinico.historia.enums.AccionAuditoria;
import com.neovision.clinico.historia.enums.EstadoAtencion;
import com.neovision.clinico.historia.repository.AdendaRepository;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.historia.repository.Cie10Repository;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.service.CitaEstudiantes;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.common.exception.AccesoNoAutorizadoHistoriaException;
import com.neovision.common.exception.AtencionNoEncontradaException;
import com.neovision.common.exception.ContenidoIncompletoException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.HistoriaInmutableException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica del ciclo de vida (workflow) de una atención: apertura, edición de
 * borrador, envío a revisión, devolución, finalización con co-firma del docente,
 * adendas y consulta. Aplica la inmutabilidad del Art. 18 y registra auditoría.
 */
@Service
public class AtencionService {

    /**
     * Días de gracia tras la fecha de la cita durante los cuales el estudiante aún
     * puede editar el borrador (principio de oportunidad, Resolución 1995/1999).
     * Pasado el plazo, el registro solo puede finalizarse o corregirse por adenda.
     */
    private static final int VENTANA_GRACIA_DIAS = 2;

    private final AtencionRepository atencionRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final CitaRepository citaRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final AdendaRepository adendaRepository;
    private final Cie10Repository cie10Repository;
    private final AuditoriaService auditoriaService;
    private final HistoriaAccessGuard accessGuard;
    private final HistoriaMapper mapper;
    // Se instancia directamente (igual que MicrosoftTokenService); el proyecto no
    // expone un bean ObjectMapper inyectable.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AtencionService(AtencionRepository atencionRepository,
                           HistoriaClinicaRepository historiaRepository,
                           CitaRepository citaRepository,
                           EstudianteRepository estudianteRepository,
                           DocenteRepository docenteRepository,
                           AdendaRepository adendaRepository,
                           Cie10Repository cie10Repository,
                           AuditoriaService auditoriaService,
                           HistoriaAccessGuard accessGuard,
                           HistoriaMapper mapper) {
        this.atencionRepository = atencionRepository;
        this.historiaRepository = historiaRepository;
        this.citaRepository = citaRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.adendaRepository = adendaRepository;
        this.cie10Repository = cie10Repository;
        this.auditoriaService = auditoriaService;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
    }

    // ===================== Helpers de autorización fina =====================

    private Estudiante estudianteActual(Authentication auth) {
        return estudianteRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new AccesoNoAutorizadoHistoriaException("Estudiante no encontrado"));
    }

    private Docente docenteActual(Authentication auth) {
        return docenteRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new AccesoNoAutorizadoHistoriaException("Docente no encontrado"));
    }

    private void requireOwnerEstudiante(Atencion a, Authentication auth) {
        // Puede editar quien abrió la atención o cualquiera de los (hasta dos)
        // estudiantes de la cita: el paciente se atiende en pareja y la atención
        // del encuentro es una sola, compartida por ambos.
        String correo = auth.getName();
        boolean esAutor = a.getEstudiante() != null && a.getEstudiante().getCorreo().equalsIgnoreCase(correo);
        boolean esDeLaPareja = CitaEstudiantes.incluyeCorreo(a.getCita(), correo);
        if (!esAutor && !esDeLaPareja) {
            throw new AccesoNoAutorizadoHistoriaException("No puedes modificar una atención que no es tuya");
        }
    }

    private void requireSupervisor(Atencion a, Authentication auth) {
        if (a.getDocenteSupervisor() == null
                || !a.getDocenteSupervisor().getCorreo().equalsIgnoreCase(auth.getName())) {
            throw new AccesoNoAutorizadoHistoriaException("Solo el docente supervisor de la sesión puede revisar esta atención");
        }
    }

    private void requireEditable(Atencion a) {
        if (a.getEstado() == EstadoAtencion.FINALIZADA) {
            throw new HistoriaInmutableException("La atención está finalizada y no puede modificarse. Usa una adenda para correcciones.");
        }
        if (a.getEstado() == EstadoAtencion.PENDIENTE_REVISION) {
            throw new AccesoNoAutorizadoHistoriaException("La atención está en revisión del docente y no es editable en este momento");
        }
        // Ventana de oportunidad: no se editan datos clínicos de una visita ya pasada.
        java.time.LocalDate fechaCita = fechaCita(a);
        if (fechaCita != null && java.time.LocalDate.now().isAfter(fechaCita.plusDays(VENTANA_GRACIA_DIAS))) {
            throw new HistoriaInmutableException(
                    "La ventana para editar este registro (fecha de la cita + " + VENTANA_GRACIA_DIAS
                    + " días) ya cerró. Finalízalo o agrega una adenda.");
        }
    }

    private java.time.LocalDate fechaCita(Atencion a) {
        return a.getCita() != null && a.getCita().getSesion() != null
                ? a.getCita().getSesion().getFecha() : null;
    }

    private Atencion buscar(Integer id) {
        return atencionRepository.findById(id)
                .orElseThrow(() -> new AtencionNoEncontradaException("Atención no encontrada"));
    }

    private Integer idPaciente(Atencion a) {
        return a.getHistoriaClinica() != null && a.getHistoriaClinica().getPaciente() != null
                ? a.getHistoriaClinica().getPaciente().getIdPaciente() : null;
    }

    // ===================== Workflow =====================

    @Transactional
    public AtencionResponse abrirAtencion(Integer idCita, Authentication auth) {
        Estudiante estudiante = estudianteActual(auth);

        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // La cita puede tener dos estudiantes (atienden en pareja): cualquiera de
        // los dos puede abrir y trabajar la unica atencion del encuentro.
        if (!CitaEstudiantes.incluye(cita, estudiante.getIdEstudiante())) {
            throw new AccesoNoAutorizadoHistoriaException("La cita no te pertenece");
        }
        Paciente paciente = cita.getPaciente();
        if (paciente == null) {
            throw new RuntimeException("La cita no tiene un paciente asignado");
        }
        HistoriaClinica historia = historiaRepository.findByPacienteIdPaciente(paciente.getIdPaciente())
                .orElseThrow(() -> new RuntimeException("El paciente no tiene historia clínica abierta. Ábrela primero."));

        // Idempotente: si ya hay una atención para esta cita, se devuelve (el
        // estudiante retoma su borrador o consulta la finalizada).
        Atencion existente = atencionRepository.findByCitaIdCita(idCita).orElse(null);
        if (existente != null) {
            // Ya se validó arriba que el estudiante es de la pareja de la cita; la
            // atención del encuentro es una sola y la comparten ambos (el campo
            // "estudiante" de la atención queda como quien la abrió primero).
            return mapper.toAtencionResponse(existente,
                    adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(existente.getIdAtencion()));
        }

        Atencion a = new Atencion();
        a.setHistoriaClinica(historia);
        a.setCita(cita);
        a.setEstudiante(estudiante);
        a.setDocenteSupervisor(cita.getSesion() != null ? cita.getSesion().getDocente() : null);
        a.setEstado(EstadoAtencion.BORRADOR);
        a.setFechaAtencion(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        prefillDatosGenerales(a.getDatosGenerales(), paciente);
        try {
            atencionRepository.save(a);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Doble clic / dos pestañas: otra petición concurrente ya creó la
            // atención de esta cita entre el chequeo de "existente" de arriba y
            // este guardado. En vez de propagar el error de restricción única,
            // se recupera esa atención y se devuelve como si fuera la propia.
            return atencionRepository.findByCitaIdCita(idCita)
                    .map(existenteConcurrente -> mapper.toAtencionResponse(existenteConcurrente,
                            adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(existenteConcurrente.getIdAtencion())))
                    .orElseThrow(() -> e);
        }

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.ABRIR_ATENCION, "ATENCION", a.getIdAtencion(),
                paciente.getIdPaciente(), "Apertura de atención");

        return mapper.toAtencionResponse(a, List.of());
    }

    private void prefillDatosGenerales(DatosGenerales dg, Paciente p) {
        dg.setApellidos(p.getApellido());
        dg.setNombres(p.getNombre());
        dg.setTipoDocumento(p.getTipoDocumento());
        dg.setNumeroDocumento(p.getNumeroDocumento());
        dg.setFechaNacimiento(p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : null);
        dg.setCelular(p.getTelefono());
    }

    @Transactional
    public AtencionResponse guardarBorrador(Integer idAtencion, GuardarAtencionRequest req, Authentication auth) {
        Atencion a = buscar(idAtencion);
        requireOwnerEstudiante(a, auth);
        requireEditable(a);

        a.setMotivoConsulta(req.getMotivoConsulta());
        if (req.getDatosGenerales() != null)        a.setDatosGenerales(req.getDatosGenerales());
        if (req.getAnamnesis() != null)             a.setAnamnesis(req.getAnamnesis());
        if (req.getSignosVitales() != null)         a.setSignosVitales(req.getSignosVitales());
        if (req.getLensometria() != null)           a.setLensometria(req.getLensometria());
        if (req.getAgudezaVisual() != null)         a.setAgudezaVisual(req.getAgudezaVisual());
        if (req.getQueratometria() != null)         a.setQueratometria(req.getQueratometria());
        if (req.getRefraccion() != null)            a.setRefraccion(req.getRefraccion());
        if (req.getPruebasComplementarias() != null) a.setPruebasComplementarias(req.getPruebasComplementarias());
        if (req.getFormulaFinal() != null)          a.setFormulaFinal(req.getFormulaFinal());
        if (req.getExamenExterno() != null)         a.setExamenExterno(req.getExamenExterno());
        if (req.getMotilidad() != null)             a.setMotilidad(req.getMotilidad());
        if (req.getFondoOjo() != null)              a.setFondoOjo(req.getFondoOjo());
        if (req.getConducta() != null)              a.setConducta(req.getConducta());

        if (req.getDiagnosticos() != null) {
            a.getDiagnosticos().clear();
            int orden = 1;
            for (DiagnosticoDto d : req.getDiagnosticos()) {
                Diagnostico diag = new Diagnostico();
                diag.setAtencion(a);
                diag.setCodigoCie10(d.getCodigoCie10());
                diag.setDescripcion(d.getDescripcion());
                diag.setOrden(d.getOrden() != null ? d.getOrden() : orden);
                a.getDiagnosticos().add(diag);
                orden++;
            }
        }

        if (req.getProcedimientos() != null) {
            a.getProcedimientos().clear();
            int orden = 1;
            for (ProcedimientoDto p : req.getProcedimientos()) {
                Procedimiento proc = new Procedimiento();
                proc.setAtencion(a);
                proc.setCodigoCups(p.getCodigoCups());
                proc.setDescripcion(p.getDescripcion());
                proc.setOrden(p.getOrden() != null ? p.getOrden() : orden);
                a.getProcedimientos().add(proc);
                orden++;
            }
        }

        a.setUpdatedAt(LocalDateTime.now());
        atencionRepository.save(a);

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.GUARDAR_BORRADOR, "ATENCION", a.getIdAtencion(),
                idPaciente(a), "Guardado de borrador");

        return mapper.toAtencionResponse(a, adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(a.getIdAtencion()));
    }

    @Transactional
    public AtencionResponse enviarRevision(Integer idAtencion, EnviarRevisionRequest req, Authentication auth) {
        Atencion a = buscar(idAtencion);
        requireOwnerEstudiante(a, auth);
        if (a.getEstado() == EstadoAtencion.FINALIZADA) {
            throw new HistoriaInmutableException("La atención ya está finalizada");
        }
        if (a.getEstado() == EstadoAtencion.PENDIENTE_REVISION) {
            throw new EstadoInvalidoException("La atención ya fue enviada a revisión");
        }
        // Nota: a propósito NO se llama requireEditable() aquí, así que la ventana de
        // gracia (VENTANA_GRACIA_DIAS) no bloquea el envío a revisión, solo la edición
        // de campos clínicos. Si se cerró la ventana, el estudiante ya no puede seguir
        // editando el borrador, pero sí debe poder enviar lo que tenga para que el
        // docente lo revise (devuelva o finalice); de lo contrario el registro quedaría
        // huérfano en BORRADOR sin ninguna salida posible.
        // Firma del estudiante: se toma la registrada en su cuenta (no la del cliente);
        // si no tiene registrada, se acepta la que venga en el request como respaldo.
        String firmaEst = a.getEstudiante().getUsuario() != null ? a.getEstudiante().getUsuario().getFirma() : null;
        if (firmaEst == null && req != null) firmaEst = req.getFirmaEstudiante();
        if (firmaEst != null) a.setFirmaEstudiante(firmaEst);

        a.setEstado(EstadoAtencion.PENDIENTE_REVISION);
        a.setFechaEnvioRevision(LocalDateTime.now());
        atencionRepository.save(a);

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.ENVIAR_REVISION, "ATENCION", a.getIdAtencion(),
                idPaciente(a), "Enviada a revisión del docente");

        return mapper.toAtencionResponse(a, adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(a.getIdAtencion()));
    }

    @Transactional
    public AtencionResponse devolver(Integer idAtencion, DevolverAtencionRequest req, Authentication auth) {
        Atencion a = buscar(idAtencion);
        requireSupervisor(a, auth);
        if (a.getEstado() != EstadoAtencion.PENDIENTE_REVISION) {
            throw new EstadoInvalidoException("Solo se pueden devolver atenciones que están en revisión");
        }
        a.setEstado(EstadoAtencion.DEVUELTA);
        a.setMotivoDevolucion(req.getMotivo());
        atencionRepository.save(a);

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.DEVOLVER, "ATENCION", a.getIdAtencion(),
                idPaciente(a), "Devuelta al estudiante: " + req.getMotivo());

        return mapper.toAtencionResponse(a, adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(a.getIdAtencion()));
    }

    @Transactional
    public AtencionResponse finalizar(Integer idAtencion, FinalizarAtencionRequest req, Authentication auth) {
        Atencion a = buscar(idAtencion);
        requireSupervisor(a, auth);
        if (a.getEstado() == EstadoAtencion.FINALIZADA) {
            throw new HistoriaInmutableException("La atención ya está finalizada");
        }
        if (a.getEstado() != EstadoAtencion.PENDIENTE_REVISION) {
            throw new EstadoInvalidoException("Solo se pueden finalizar atenciones que están en revisión");
        }
        // Contenido clínico mínimo: una vez finalizada, la atención es inmutable.
        // No se permite sellar un registro vacío.
        validarContenidoMinimo(a);
        // Firma del docente: la registrada en su cuenta (no la del cliente); respaldo = request.
        String firmaDoc = a.getDocenteSupervisor().getUsuario() != null ? a.getDocenteSupervisor().getUsuario().getFirma() : null;
        if (firmaDoc == null && req != null) firmaDoc = req.getFirmaDocente();
        // La co-firma del docente es obligatoria: sin ella, finalizar dejaría un
        // registro inmutable sin evidencia de la supervisión exigida por el flujo.
        if (firmaDoc == null) {
            throw new ContenidoIncompletoException(
                    "No se puede finalizar: no tienes una firma registrada. "
                    + "Regístrala en tu perfil (Mi perfil → Firma y sello) antes de finalizar.");
        }
        a.setFirmaDocente(firmaDoc);
        // La firma del paciente sí viene del cliente (firma presencial en el dispositivo).
        if (req != null && req.getFirmaPaciente() != null) a.setFirmaPaciente(req.getFirmaPaciente());

        a.setEstado(EstadoAtencion.FINALIZADA);
        a.setFechaFinalizacion(LocalDateTime.now());
        a.setHashIntegridad(calcularHash(a));
        atencionRepository.save(a);

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.FINALIZAR, "ATENCION", a.getIdAtencion(),
                idPaciente(a), "Atención finalizada y firmada (hash " + a.getHashIntegridad().substring(0, 12) + "...)");

        return mapper.toAtencionResponse(a, adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(a.getIdAtencion()));
    }

    private static final java.util.regex.Pattern EMOJI_PATTERN = java.util.regex.Pattern.compile(
            "[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]");

    /** Exige contenido clínico mínimo antes de sellar la atención como FINALIZADA. */
    private void validarContenidoMinimo(Atencion a) {
        if (a.getMotivoConsulta() == null || a.getMotivoConsulta().isBlank()) {
            throw new ContenidoIncompletoException("No se puede finalizar: falta el motivo de consulta.");
        }
        // Datos ilogicos o corruptos no deben quedar sellados en un registro inmutable.
        if (a.getDatosGenerales() != null) {
            validarSinEmojis(a.getDatosGenerales().getNombres(), "nombre del paciente");
            validarSinEmojis(a.getDatosGenerales().getApellidos(), "apellido del paciente");
        }
        if (a.getSignosVitales() != null) {
            validarRangoFisiologico(a.getSignosVitales().getPeso(), 0.5, 300, "peso (kg)");
            validarRangoFisiologico(a.getSignosVitales().getEstatura(), 30, 250, "estatura (cm)");
            validarRangoFisiologico(a.getSignosVitales().getImc(), 10, 80, "IMC");
        }
        if (a.getDiagnosticos() == null || a.getDiagnosticos().isEmpty()) {
            throw new ContenidoIncompletoException(
                    "No se puede finalizar: debe registrar al menos un diagnóstico (CIE-10).");
        }
        // Vocabulario controlado: cada diagnóstico debe ser un código CIE-10 válido.
        for (Diagnostico d : a.getDiagnosticos()) {
            if (d.getCodigoCie10() == null || d.getCodigoCie10().isBlank()
                    || !cie10Repository.existsByCodigoAndActivoTrue(d.getCodigoCie10().trim())) {
                throw new ContenidoIncompletoException(
                        "Diagnóstico con código CIE-10 inválido: '" + d.getCodigoCie10()
                        + "'. Selecciónalo del catálogo.");
            }
        }
    }

    private void validarSinEmojis(String valor, String campo) {
        if (valor != null && EMOJI_PATTERN.matcher(valor).find()) {
            throw new ContenidoIncompletoException("El " + campo + " no puede contener emojis.");
        }
    }

    /**
     * Rechaza valores numericos fuera de un rango fisiologicamente posible (ej.
     * un peso de 1000 kg). Los valores no numericos (texto libre, observaciones)
     * se dejan pasar sin validar por rango.
     */
    private void validarRangoFisiologico(String valor, double min, double max, String campo) {
        if (valor == null || valor.isBlank()) return;
        double v;
        try {
            v = Double.parseDouble(valor.trim().replace(",", "."));
        } catch (NumberFormatException ignored) {
            return;
        }
        if (v < min || v > max) {
            throw new ContenidoIncompletoException(
                    "El valor de " + campo + " (" + valor + ") esta fuera de un rango fisiologicamente posible ("
                    + min + " a " + max + ").");
        }
    }

    /** Sello de integridad SHA-256 del contenido clínico canónico de la atención. */
    private String calcularHash(Atencion a) {
        try {
            Object[] contenido = {
                    a.getMotivoConsulta(), a.getDatosGenerales(), a.getAnamnesis(), a.getSignosVitales(),
                    a.getLensometria(), a.getAgudezaVisual(), a.getQueratometria(), a.getRefraccion(),
                    a.getPruebasComplementarias(), a.getFormulaFinal(), a.getExamenExterno(),
                    a.getMotilidad(), a.getFondoOjo(), a.getConducta(),
                    a.getDiagnosticos().stream().map(mapper::toDiagnosticoDto).toList(),
                    a.getProcedimientos().stream().map(mapper::toProcedimientoDto).toList()
            };
            return HashUtil.sha256Hex(objectMapper.writeValueAsString(contenido));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular el sello de integridad de la atención", e);
        }
    }

    @Transactional
    public AtencionResponse obtenerDetalle(Integer idAtencion, Authentication auth) {
        Atencion a = buscar(idAtencion);
        accessGuard.requireLecturaAtencion(a, auth);
        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.CONSULTAR, "ATENCION", a.getIdAtencion(),
                idPaciente(a), "Consulta de atención");
        return mapper.toAtencionResponse(a, adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(a.getIdAtencion()));
    }

    /**
     * Verifica el sello de integridad: recalcula el hash del contenido actual y lo
     * compara con el almacenado al finalizar. Permite detectar manipulación directa
     * en la base de datos (refuerza el Art. 18 de la Resolución 1995/1999).
     */
    @Transactional
    public VerificacionIntegridadResponse verificarIntegridad(Integer idAtencion, Authentication auth) {
        Atencion a = buscar(idAtencion);
        accessGuard.requireLecturaAtencion(a, auth);
        if (a.getEstado() != EstadoAtencion.FINALIZADA || a.getHashIntegridad() == null) {
            return new VerificacionIntegridadResponse(false, false, null, null,
                    "La atención no está finalizada; aún no tiene sello de integridad.");
        }
        String recalculado = calcularHash(a);
        boolean valido = recalculado.equals(a.getHashIntegridad());
        return new VerificacionIntegridadResponse(true, valido, a.getHashIntegridad(), recalculado,
                valido ? "Íntegra: el contenido coincide con el sello original."
                       : "ALTERADA: el contenido no coincide con el sello de integridad original.");
    }

    @Transactional(readOnly = true)
    public List<AtencionResumenResponse> misAtenciones(Authentication auth) {
        Estudiante estudiante = estudianteActual(auth);
        return atencionRepository
                .findByEstudianteIdEstudianteOrderByFechaAtencionDesc(estudiante.getIdEstudiante())
                .stream().map(mapper::toResumen).toList();
    }

    @Transactional(readOnly = true)
    public List<AtencionResumenResponse> bandejaRevision(Authentication auth) {
        Docente docente = docenteActual(auth);
        return atencionRepository
                .findByDocenteSupervisorIdDocenteAndEstadoOrderByFechaEnvioRevisionAsc(
                        docente.getIdDocente(), EstadoAtencion.PENDIENTE_REVISION)
                .stream().map(mapper::toResumen).toList();
    }

    @Transactional
    public AdendaResponse agregarAdenda(Integer idAtencion, AdendaRequest req, Authentication auth) {
        Atencion a = buscar(idAtencion);
        if (a.getEstado() != EstadoAtencion.FINALIZADA) {
            throw new RuntimeException("Solo se pueden agregar adendas a atenciones finalizadas");
        }
        Adenda adenda = new Adenda();
        adenda.setAtencion(a);
        adenda.setTexto(req.getTexto());
        adenda.setAutorCorreo(auth.getName());
        adenda.setAutorRol(HistoriaClinicaService.rol(auth));
        adenda.setFecha(LocalDateTime.now());
        adendaRepository.save(adenda);

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.CREAR_ADENDA, "ADENDA", adenda.getIdAdenda(),
                idPaciente(a), "Adenda agregada a atención #" + a.getIdAtencion());

        return mapper.toAdendaResponse(adenda);
    }
}
