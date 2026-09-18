package com.neovision.admin.service;

import com.neovision.admin.dto.CrearDocenteRequest;
import com.neovision.admin.dto.CrearEstudianteRequest;
import com.neovision.admin.dto.CrearPacienteRequest;
import com.neovision.admin.dto.DocenteResponse;
import com.neovision.admin.dto.EditarDocenteRequest;
import com.neovision.admin.dto.EditarDocenteAdminRequest;
import com.neovision.admin.dto.EditarEstudianteRequest;
import com.neovision.admin.dto.EditarEstudianteAdminRequest;
import com.neovision.admin.dto.EditarPacienteRequest;
import com.neovision.admin.dto.EditarPacienteAdminRequest;
import com.neovision.admin.dto.EstudianteResponse;
import com.neovision.admin.dto.PacienteResponse;
import com.neovision.clinico.dto.EspecialidadResponse;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Especialidad;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EspecialidadRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.CorreoYaRegistradoException;
import com.neovision.common.exception.DocenteNoEncontradoException;
import com.neovision.common.exception.DocumentoDuplicadoException;
import com.neovision.common.exception.EspecialidadNoEncontradaException;
import com.neovision.common.exception.EstudianteNoEncontradoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import com.neovision.common.exception.RolNoEncontradoException;
import com.neovision.common.validation.ValidacionesUsuario;
import com.neovision.usuario.entity.Rol;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.RolRepository;
import com.neovision.usuario.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Schema(description = "Crea la logica de negocio, como *Crud*. (la logica en si, como funciona cada metodo del crud, validaciones, etc.)")
public class AdminServiceImpl implements AdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final EspecialidadRepository especialidadRepository;
    private final ValidacionesUsuario validaciones;

    public AdminServiceImpl(UsuarioRepository usuarioRepository,
                            RolRepository rolRepository,
                            DocenteRepository docenteRepository,
                            EstudianteRepository estudianteRepository,
                            PacienteRepository pacienteRepository,
                            PasswordEncoder passwordEncoder,
                            EspecialidadRepository especialidadRepository,
                            ValidacionesUsuario validaciones) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.especialidadRepository = especialidadRepository;
        this.validaciones = validaciones;
    }

    // --- Docentes ---

    @Override
    @Transactional
    public DocenteResponse crearDocente(CrearDocenteRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoYaRegistradoException("El correo " + request.getCorreo() + " ya está registrado");
        }
        validaciones.validarCorreoInstitucional(request.getCorreo(), "docente");
        if (request.getCedula() != null && !request.getCedula().isBlank()) {
            validarCedulaUnica(request.getCedula(), "DOCENTE", null);
        }
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");

        Rol rolDocente = rolRepository.findByNombreRol("ROLE_DOCENTE")
                .orElseThrow(() -> new RolNoEncontradoException("Rol DOCENTE no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setContrasena(null);
        usuario.setEstado(true);
        usuario.setRol(rolDocente);
        usuarioRepository.save(usuario);

        Docente docente = new Docente();
        docente.setNombre(request.getNombre());
        docente.setApellido(request.getApellido());
        docente.setCorreo(request.getCorreo());
        docente.setCedula(request.getCedula());
        docente.setTelefono(request.getTelefono());
        docente.setUsuario(usuario);

        // Asignar especialidades: las indicadas + siempre la default (FUNCIONAL).
        aplicarEspecialidades(docente, request.getIdsEspecialidades());

        docenteRepository.save(docente);

        return toDocenteResponse(docente, usuario);
    }

    private void aplicarEspecialidades(Docente docente, java.util.List<Integer> idsEspecialidades) {
        java.util.Set<Especialidad> nuevas = new java.util.HashSet<>();
        if (idsEspecialidades != null) {
            for (Integer idEsp : idsEspecialidades) {
                if (idEsp == null) continue;
                Especialidad e = especialidadRepository.findById(idEsp)
                        .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada: " + idEsp));
                nuevas.add(e);
            }
        }
        // FUNCIONAL siempre se asigna (todos los docentes pueden hacerla).
        for (Especialidad def : especialidadRepository.findByEsDefaultTrue()) {
            nuevas.add(def);
        }
        if (docente.getEspecialidades() == null) {
            docente.setEspecialidades(new java.util.HashSet<>());
        }
        docente.getEspecialidades().clear();
        docente.getEspecialidades().addAll(nuevas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocenteResponse> listarDocentes() {
        return docenteRepository.findAll().stream()
                .map(d -> toDocenteResponse(d, d.getUsuario()))
                .toList();
    }

    @Override
    @Transactional
    public DocenteResponse editarDocente(Integer id, EditarDocenteRequest request) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        docente.setNombre(request.getNombre());
        docente.setApellido(request.getApellido());
        docente.setTelefono(request.getTelefono());
        docenteRepository.save(docente);
        return toDocenteResponse(docente, docente.getUsuario());
    }

    @Override
    @Transactional
    public DocenteResponse editarDocente(Integer id, EditarDocenteAdminRequest request) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        validaciones.validarCorreoInstitucional(request.getCorreo(), "docente");
        if (request.getCedula() != null && !request.getCedula().isBlank()) {
            validarCedulaUnica(request.getCedula(), "DOCENTE", id);
        }
        docente.setNombre(request.getNombre());
        docente.setApellido(request.getApellido());
        docente.setCedula(request.getCedula());
        docente.setTelefono(request.getTelefono());
        docente.setCorreo(request.getCorreo());
        docente.getUsuario().setCorreo(request.getCorreo());
        // Si vienen especialidades en el request, reemplazar las del docente
        if (request.getIdsEspecialidades() != null) {
            aplicarEspecialidades(docente, request.getIdsEspecialidades());
        }
        docenteRepository.save(docente);
        usuarioRepository.save(docente.getUsuario());
        return toDocenteResponse(docente, docente.getUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse obtenerDocente(Integer id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        return toDocenteResponse(docente, docente.getUsuario());
    }

    @Override
    @Transactional
    public void desactivarDocente(Integer id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        docente.getUsuario().setEstado(false);
        usuarioRepository.save(docente.getUsuario());
    }

    @Override
    @Transactional
    public void eliminarDocente(Integer id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        try {
            docenteRepository.delete(docente);
            docenteRepository.flush();
            usuarioRepository.delete(docente.getUsuario());
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: el docente tiene sesiones o datos asociados. Desactívalo en su lugar.");
        }
    }

    // --- Estudiantes ---

    @Override
    @Transactional
    public EstudianteResponse crearEstudiante(CrearEstudianteRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoYaRegistradoException("El correo " + request.getCorreo() + " ya está registrado");
        }
        validaciones.validarCorreoInstitucional(request.getCorreo(), "estudiante");

        if (request.getCedula() != null && !request.getCedula().isBlank()) {
            validarCedulaUnica(request.getCedula(), "ESTUDIANTE", null);
        }
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");

        Rol rolEstudiante = rolRepository.findByNombreRol("ROLE_ESTUDIANTE")
                .orElseThrow(() -> new RolNoEncontradoException("Rol ESTUDIANTE no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setContrasena(null);
        usuario.setEstado(true);
        usuario.setRol(rolEstudiante);
        usuarioRepository.save(usuario);

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setCorreo(request.getCorreo());
        estudiante.setCedula(request.getCedula());
        estudiante.setSemestre(request.getSemestre());
        estudiante.setTelefono(request.getTelefono());
        estudiante.setUsuario(usuario);
        estudianteRepository.save(estudiante);

        return toEstudianteResponse(estudiante, usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarEstudiantes() {
        return estudianteRepository.findAll().stream()
                .map(e -> toEstudianteResponse(e, e.getUsuario()))
                .toList();
    }

    @Override
    @Transactional
    public EstudianteResponse editarEstudiante(Integer id, EditarEstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setTelefono(request.getTelefono());
        estudianteRepository.save(estudiante);
        return toEstudianteResponse(estudiante, estudiante.getUsuario());
    }

    @Override
    @Transactional
    public EstudianteResponse editarEstudiante(Integer id, EditarEstudianteAdminRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        validaciones.validarCorreoInstitucional(request.getCorreo(), "estudiante");
        if (request.getCedula() != null && !request.getCedula().isBlank()) {
            validarCedulaUnica(request.getCedula(), "ESTUDIANTE", id);
        }
        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setCedula(request.getCedula());
        estudiante.setSemestre(request.getSemestre());
        estudiante.setTelefono(request.getTelefono());
        estudiante.setCorreo(request.getCorreo());
        estudiante.getUsuario().setCorreo(request.getCorreo());
        estudianteRepository.save(estudiante);
        usuarioRepository.save(estudiante.getUsuario());
        return toEstudianteResponse(estudiante, estudiante.getUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtenerEstudiante(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        return toEstudianteResponse(estudiante, estudiante.getUsuario());
    }

    @Override
    @Transactional
    public void desactivarEstudiante(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        estudiante.getUsuario().setEstado(false);
        usuarioRepository.save(estudiante.getUsuario());
    }

    @Override
    @Transactional
    public void eliminarEstudiante(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        try {
            estudianteRepository.delete(estudiante);
            estudianteRepository.flush();
            usuarioRepository.delete(estudiante.getUsuario());
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: el estudiante tiene citas o sesiones asociadas. Desactívalo en su lugar.");
        }
    }

    // --- Pacientes ---

    @Override
    @Transactional
    public PacienteResponse crearPaciente(CrearPacienteRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoYaRegistradoException("El correo " + request.getCorreo() + " ya está registrado");
        }

        validaciones.validarTelefono(request.getTelefono());
        LocalDate nacimiento = LocalDate.parse(request.getFechaNacimiento());
        validaciones.validarTipoDocumentoEdad(request.getTipoDocumento(), nacimiento);
        if (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank()) {
            validarCedulaUnica(request.getNumeroDocumento(), "PACIENTE", null);
        }
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        validaciones.validarContrasena(request.getContrasena());
        validaciones.validarRepresentanteLegalSiMenor(request.getTipoDocumento(),
                request.getRepresentanteNombre(), request.getRepresentanteDocumento(), request.getRepresentanteParentesco());

        Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                .orElseThrow(() -> new RolNoEncontradoException("Rol PACIENTE no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setEstado(true);
        usuario.setRol(rolPaciente);
        usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setNombre(request.getNombre());
        paciente.setApellido(request.getApellido());
        paciente.setTipoDocumento(request.getTipoDocumento());
        paciente.setNumeroDocumento(request.getNumeroDocumento());
        paciente.setFechaNacimiento(LocalDate.parse(request.getFechaNacimiento()));
        paciente.setTelefono(request.getTelefono());
        paciente.setCorreo(request.getCorreo());
        paciente.setUsuario(usuario);
        if (Boolean.TRUE.equals(request.getAutorizaTratamientoDatos())) {
            paciente.setAutorizaTratamientoDatos(true);
            paciente.setFechaAutorizacionDatos(java.time.LocalDateTime.now());
        }
        paciente.setRepresentanteNombre(request.getRepresentanteNombre());
        paciente.setRepresentanteDocumento(request.getRepresentanteDocumento());
        paciente.setRepresentanteParentesco(request.getRepresentanteParentesco());
        pacienteRepository.save(paciente);

        return toPacienteResponse(paciente, usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> listarPacientes() {
        return pacienteRepository.findAll().stream()
                .map(p -> toPacienteResponse(p, p.getUsuario()))
                .toList();
    }

    @Override
    @Transactional
    public PacienteResponse editarPaciente(Integer id, EditarPacienteRequest request) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        validaciones.validarTelefono(request.getTelefono());
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        paciente.setNombre(request.getNombre());
        paciente.setApellido(request.getApellido());
        paciente.setTelefono(request.getTelefono());
        pacienteRepository.save(paciente);
        return toPacienteResponse(paciente, paciente.getUsuario());
    }

    @Override
    @Transactional
    public PacienteResponse editarPaciente(Integer id, EditarPacienteAdminRequest request) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        validaciones.validarTelefono(request.getTelefono());
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        LocalDate nacimiento = LocalDate.parse(request.getFechaNacimiento());
        validaciones.validarTipoDocumentoEdad(request.getTipoDocumento(), nacimiento);
        validaciones.validarRepresentanteLegalSiMenor(request.getTipoDocumento(),
                request.getRepresentanteNombre(), request.getRepresentanteDocumento(), request.getRepresentanteParentesco());
        if (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank()) {
            validarCedulaUnica(request.getNumeroDocumento(), "PACIENTE", id);
        }
        paciente.setNombre(request.getNombre());
        paciente.setApellido(request.getApellido());
        paciente.setTipoDocumento(request.getTipoDocumento());
        paciente.setNumeroDocumento(request.getNumeroDocumento());
        paciente.setFechaNacimiento(nacimiento);
        paciente.setTelefono(request.getTelefono());
        paciente.setCorreo(request.getCorreo());
        paciente.getUsuario().setCorreo(request.getCorreo());
        paciente.setRepresentanteNombre(request.getRepresentanteNombre());
        paciente.setRepresentanteDocumento(request.getRepresentanteDocumento());
        paciente.setRepresentanteParentesco(request.getRepresentanteParentesco());
        pacienteRepository.save(paciente);
        usuarioRepository.save(paciente.getUsuario());
        return toPacienteResponse(paciente, paciente.getUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse obtenerPaciente(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        return toPacienteResponse(paciente, paciente.getUsuario());
    }

    @Override
    @Transactional
    public void desactivarPaciente(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        paciente.getUsuario().setEstado(false);
        usuarioRepository.save(paciente.getUsuario());
    }

    /**
     * Restablece la contrasena de un paciente. No hay flujo de "olvide mi
     * contrasena" por correo (el envio de emails no esta configurado en este
     * entorno); en su lugar, solo el admin/secretaria puede fijar una nueva
     * contrasena directamente desde el panel.
     */
    @Override
    @Transactional
    public void resetearContrasenaPaciente(Integer id, String nuevaContrasena) {
        if (nuevaContrasena == null || nuevaContrasena.length() < 12) {
            throw new RuntimeException("La contraseña debe tener al menos 12 caracteres");
        }
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        validaciones.validarContrasena(nuevaContrasena);
        paciente.getUsuario().setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(paciente.getUsuario());
    }

    @Override
    @Transactional
    public void eliminarPaciente(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        try {
            pacienteRepository.delete(paciente);
            pacienteRepository.flush();
            usuarioRepository.delete(paciente.getUsuario());
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: el paciente tiene citas asociadas. Desactívalo en su lugar.");
        }
    }

    // --- Mappers ---

    private DocenteResponse toDocenteResponse(Docente docente, Usuario usuario) {
        java.util.List<EspecialidadResponse> especialidades = java.util.Collections.emptyList();
        if (docente.getEspecialidades() != null) {
            especialidades = docente.getEspecialidades().stream()
                    .map(e -> new EspecialidadResponse(
                            e.getIdEspecialidad(), e.getNombre(),
                            e.getObservacionRecordatorio(),
                            e.getRequiereSubtipo(), e.getEsDefault(), e.getActivo()))
                    .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                    .toList();
        }
        return new DocenteResponse(
                docente.getIdDocente(),
                docente.getNombre(),
                docente.getApellido(),
                docente.getCorreo(),
                docente.getCedula(),
                docente.getTelefono(),
                usuario.getEstado(),
                especialidades
        );
    }

    private EstudianteResponse toEstudianteResponse(Estudiante estudiante, Usuario usuario) {
        return new EstudianteResponse(
                estudiante.getIdEstudiante(),
                estudiante.getNombre(),
                estudiante.getApellido(),
                estudiante.getCorreo(),
                estudiante.getCedula(),
                estudiante.getSemestre(),
                estudiante.getTelefono(),
                usuario.getEstado()
        );
    }

    private PacienteResponse toPacienteResponse(Paciente paciente, Usuario usuario) {
        return new PacienteResponse(
                paciente.getIdPaciente(),
                paciente.getNombre(),
                paciente.getApellido(),
                paciente.getCorreo(),
                paciente.getTipoDocumento(),
                paciente.getNumeroDocumento(),
                paciente.getFechaNacimiento().toString(),
                paciente.getTelefono(),
                usuario.getEstado(),
                paciente.getAutorizaTratamientoDatos(),
                paciente.getRepresentanteNombre(),
                paciente.getRepresentanteDocumento(),
                paciente.getRepresentanteParentesco()
        );
    }

    // ===================== REACTIVACION =====================

    @Override
    @Transactional
    public void reactivarDocente(Integer id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        docente.getUsuario().setEstado(true);
        usuarioRepository.save(docente.getUsuario());
    }

    @Override
    @Transactional
    public void reactivarEstudiante(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EstudianteNoEncontradoException("Estudiante no encontrado"));
        estudiante.getUsuario().setEstado(true);
        usuarioRepository.save(estudiante.getUsuario());
    }

    @Override
    @Transactional
    public void reactivarPaciente(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        paciente.getUsuario().setEstado(true);
        usuarioRepository.save(paciente.getUsuario());
    }

    // ===================== VALIDACIONES =====================

    /**
     * Valida que la cedula no este en uso por otro docente, estudiante o paciente.
     * Si se esta editando, pasar idActual (y tipo) para excluir ese registro.
     */
    private void validarCedulaUnica(String cedula, String tipoActual, Integer idActual) {
        // Validar unicidad SOLO dentro del mismo rol, no globalmente
        if ("DOCENTE".equals(tipoActual)) {
            boolean existe = idActual != null
                    ? docenteRepository.existsByCedulaAndIdDocenteNot(cedula, idActual)
                    : docenteRepository.existsByCedula(cedula);
            if (existe) {
                throw new DocumentoDuplicadoException("La cedula " + cedula + " ya está registrada en otro docente");
            }
        } else if ("ESTUDIANTE".equals(tipoActual)) {
            boolean existe = idActual != null
                    ? estudianteRepository.existsByCedulaAndIdEstudianteNot(cedula, idActual)
                    : estudianteRepository.existsByCedula(cedula);
            if (existe) {
                throw new DocumentoDuplicadoException("La cedula " + cedula + " ya está registrada en otro estudiante");
            }
        } else if ("PACIENTE".equals(tipoActual)) {
            boolean existe = idActual != null
                    ? pacienteRepository.existsByNumeroDocumentoAndIdPacienteNot(cedula, idActual)
                    : pacienteRepository.existsByNumeroDocumento(cedula);
            if (existe) {
                throw new DocumentoDuplicadoException("El documento " + cedula + " ya está registrado en otro paciente");
            }
        }
    }
}
