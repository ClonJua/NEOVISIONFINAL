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
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.EstudianteNoEncontradoException;
import com.neovision.common.exception.PacienteNoEncontradoException;
import com.neovision.common.exception.RolNoEncontradoException;
import com.neovision.common.validation.ValidacionesUsuario;
import com.neovision.usuario.entity.Rol;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.RolRepository;
import com.neovision.usuario.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.media.Schema;

import com.neovision.admin.dto.CargaMasivaResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Schema(description = "Crea la logica de negocio, como *Crud*. (la logica en si, como funciona cada metodo del crud, validaciones, etc.)")
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final EspecialidadRepository especialidadRepository;
    private final ValidacionesUsuario validaciones;
    private final Validator validator;
    // Proxy de este mismo bean. La carga masiva llama crearDocente/crearEstudiante
    // a traves de el (no de this) para que @Transactional si aplique: Spring no
    // intercepta llamadas internas, y sin esto una fila podria dejar un Usuario
    // guardado sin su perfil si fallara a mitad de camino.
    private final AdminService self;

    public AdminServiceImpl(UsuarioRepository usuarioRepository,
                            RolRepository rolRepository,
                            DocenteRepository docenteRepository,
                            EstudianteRepository estudianteRepository,
                            PacienteRepository pacienteRepository,
                            PasswordEncoder passwordEncoder,
                            EspecialidadRepository especialidadRepository,
                            ValidacionesUsuario validaciones,
                            Validator validator,
                            @Lazy AdminService self) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.especialidadRepository = especialidadRepository;
        this.validaciones = validaciones;
        this.validator = validator;
        this.self = self;
    }

    // --- Docentes ---

    @Override
    @Transactional
    public DocenteResponse crearDocente(CrearDocenteRequest request) {
        request.setCorreo(ValidacionesUsuario.normalizarCorreo(request.getCorreo()));
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
        request.setCorreo(ValidacionesUsuario.normalizarCorreo(request.getCorreo()));
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
        request.setCorreo(ValidacionesUsuario.normalizarCorreo(request.getCorreo()));
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
        request.setCorreo(ValidacionesUsuario.normalizarCorreo(request.getCorreo()));
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
        // El correo es opcional: hay pacientes (tipicamente adultos mayores) que no
        // tienen. Sin correo no se crea cuenta de acceso — el paciente existe solo
        // como sujeto clinico y la secretaria opera por el.
        String correo = normalizarCorreoOpcional(request.getCorreo());
        request.setCorreo(correo);

        if (correo != null) {
            validaciones.validarCorreoNoInstitucional(correo);
            if (usuarioRepository.existsByCorreo(correo)) {
                throw new CorreoYaRegistradoException("El correo " + correo + " ya está registrado");
            }
        }

        validaciones.validarTelefono(request.getTelefono());
        LocalDate nacimiento = LocalDate.parse(request.getFechaNacimiento());
        validaciones.validarTipoDocumentoEdad(request.getTipoDocumento(), nacimiento);
        if (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank()) {
            validarCedulaUnica(request.getNumeroDocumento(), "PACIENTE", null);
        }
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        validaciones.validarRepresentanteLegalSiMenor(request.getTipoDocumento(),
                request.getRepresentanteNombre(), request.getRepresentanteDocumento(), request.getRepresentanteParentesco());

        Usuario usuario = null;
        if (correo != null) {
            String contrasena = request.getContrasena();
            if (contrasena == null || contrasena.length() < 12) {
                throw new RuntimeException("La contraseña debe tener al menos 12 caracteres");
            }
            validaciones.validarContrasena(contrasena);

            Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                    .orElseThrow(() -> new RolNoEncontradoException("Rol PACIENTE no encontrado"));

            usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setContrasena(passwordEncoder.encode(contrasena));
            usuario.setEstado(true);
            usuario.setRol(rolPaciente);
            usuarioRepository.save(usuario);
        }

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

        Usuario cuenta = paciente.getUsuario();
        String correo = normalizarCorreoOpcional(request.getCorreo());

        // Si tiene cuenta, el correo es su usuario de ingreso: no puede quedar vacio.
        if (cuenta != null && correo == null) {
            throw new RuntimeException("Este paciente tiene cuenta de acceso: el correo es su usuario de ingreso "
                    + "y no se puede dejar vacío.");
        }
        if (correo != null) {
            validaciones.validarCorreoNoInstitucional(correo);
            // Solo validar unicidad si realmente cambio: si no, chocaria consigo mismo.
            if (!correo.equalsIgnoreCase(paciente.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
                throw new CorreoYaRegistradoException("El correo " + correo + " ya está registrado");
            }
        }

        paciente.setNombre(request.getNombre());
        paciente.setApellido(request.getApellido());
        paciente.setTipoDocumento(request.getTipoDocumento());
        paciente.setNumeroDocumento(request.getNumeroDocumento());
        paciente.setFechaNacimiento(nacimiento);
        paciente.setTelefono(request.getTelefono());
        paciente.setCorreo(correo);
        paciente.setRepresentanteNombre(request.getRepresentanteNombre());
        paciente.setRepresentanteDocumento(request.getRepresentanteDocumento());
        paciente.setRepresentanteParentesco(request.getRepresentanteParentesco());
        pacienteRepository.save(paciente);

        if (cuenta != null) {
            // El correo tambien identifica la cuenta: hay que moverlo en los dos sitios.
            // Los tokens ya emitidos con el correo anterior dejan de resolver, asi que
            // el paciente debera volver a iniciar sesion.
            cuenta.setCorreo(correo);
            usuarioRepository.save(cuenta);
        }
        return toPacienteResponse(paciente, cuenta);
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
        Usuario cuenta = requireCuenta(paciente, "desactivar");
        cuenta.setEstado(false);
        usuarioRepository.save(cuenta);
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
        Usuario cuenta = requireCuenta(paciente, "restablecer la contraseña de");
        cuenta.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(cuenta);
    }

    @Override
    @Transactional
    public void eliminarPaciente(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("Paciente no encontrado"));
        try {
            Usuario cuenta = paciente.getUsuario();
            pacienteRepository.delete(paciente);
            pacienteRepository.flush();
            // Un paciente registrado presencialmente no tiene cuenta que borrar.
            if (cuenta != null) {
                usuarioRepository.delete(cuenta);
            }
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
                // Sin cuenta no hay estado que consultar: el paciente existe como
                // sujeto clinico y se considera activo mientras no se elimine.
                usuario != null ? usuario.getEstado() : Boolean.TRUE,
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
        Usuario cuenta = requireCuenta(paciente, "reactivar");
        cuenta.setEstado(true);
        usuarioRepository.save(cuenta);
    }

    // ===================== CARGA MASIVA (EXCEL) =====================

    private static final int MAX_FILAS_CARGA_MASIVA = 500;

    private record FilaExcel(int numeroFila, String[] valores) {}

    @Override
    public CargaMasivaResponse cargarDocentesDesdeExcel(MultipartFile archivo) {
        List<FilaExcel> filas = leerFilasExcel(archivo, 6);
        int creados = 0;
        List<CargaMasivaResponse.Incidencia> errores = new ArrayList<>();
        List<CargaMasivaResponse.Incidencia> advertencias = new ArrayList<>();

        for (FilaExcel fila : filas) {
            String[] v = fila.valores();
            try {
                CrearDocenteRequest request = new CrearDocenteRequest();
                request.setNombre(v[0]);
                request.setApellido(v[1]);
                request.setCorreo(v[2]);
                request.setCedula(v[3].isBlank() ? null : v[3]);
                request.setTelefono(v[4].isBlank() ? null : v[4]);

                List<Integer> idsEspecialidades = new ArrayList<>();
                List<String> noEncontradas = new ArrayList<>();
                for (String nombre : v[5].split(",")) {
                    String limpio = nombre.trim();
                    if (limpio.isEmpty()) continue;
                    especialidadRepository.findByNombreIgnoreCase(limpio).ifPresentOrElse(
                            e -> idsEspecialidades.add(e.getIdEspecialidad()),
                            () -> noEncontradas.add(limpio));
                }
                request.setIdsEspecialidades(idsEspecialidades);

                validarBean(request);
                self.crearDocente(request);
                creados++;
                if (!noEncontradas.isEmpty()) {
                    advertencias.add(new CargaMasivaResponse.Incidencia(fila.numeroFila(),
                            "Creado, pero no existe(n) en el catálogo: " + String.join(", ", noEncontradas)
                                    + ". Asígnala(s) desde Editar."));
                }
            } catch (RuntimeException e) {
                errores.add(new CargaMasivaResponse.Incidencia(fila.numeroFila(), e.getMessage()));
            }
        }
        return new CargaMasivaResponse(filas.size(), creados, errores, advertencias);
    }

    @Override
    public byte[] descargarPlantillaDocentes() {
        return generarPlantillaExcel("Docentes",
                new String[]{"Nombre", "Apellido", "Correo", "Cedula", "Telefono", "Especialidades"},
                new String[]{"Carlos", "Ramírez", "c.ramirez@unbosque.edu.co", "12345678", "3001234567", "Ortoptica, Control"},
                "Correo institucional @unbosque.edu.co. Cédula y Teléfono son opcionales. "
                        + "Especialidades: nombres del catalogo separados por coma (Funcional se asigna siempre).");
    }

    @Override
    public CargaMasivaResponse cargarEstudiantesDesdeExcel(MultipartFile archivo) {
        List<FilaExcel> filas = leerFilasExcel(archivo, 6);
        int creados = 0;
        List<CargaMasivaResponse.Incidencia> errores = new ArrayList<>();

        for (FilaExcel fila : filas) {
            String[] v = fila.valores();
            try {
                CrearEstudianteRequest request = new CrearEstudianteRequest();
                request.setNombre(v[0]);
                request.setApellido(v[1]);
                request.setCorreo(v[2]);
                request.setCedula(v[3]);
                if (!v[4].isBlank()) {
                    try {
                        request.setSemestre(Integer.parseInt(v[4]));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("semestre: debe ser 7, 8 o 9");
                    }
                }
                request.setTelefono(v[5].isBlank() ? null : v[5]);

                validarBean(request);
                self.crearEstudiante(request);
                creados++;
            } catch (RuntimeException e) {
                errores.add(new CargaMasivaResponse.Incidencia(fila.numeroFila(), e.getMessage()));
            }
        }
        return new CargaMasivaResponse(filas.size(), creados, errores, List.of());
    }

    @Override
    public byte[] descargarPlantillaEstudiantes() {
        return generarPlantillaExcel("Estudiantes",
                new String[]{"Nombre", "Apellido", "Correo", "Cedula", "Semestre", "Telefono"},
                new String[]{"Valeria", "López", "v.lopez@unbosque.edu.co", "1020304050", "7", "3009876543"},
                "Correo institucional @unbosque.edu.co. Cédula obligatoria. Semestre: 7, 8 o 9. Teléfono opcional.");
    }

    /**
     * Lee la primera hoja y devuelve las filas con datos (omite la cabecera y las
     * filas vacias). numeroFila es el que muestra Excel, para ubicar rapido la fila
     * con error. DataFormatter entrega las celdas numericas tal como se ven
     * (cedulas y telefonos sin ".0" ni notacion cientifica).
     */
    private List<FilaExcel> leerFilasExcel(MultipartFile archivo, int columnas) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("Selecciona un archivo Excel (.xlsx)");
        }
        DataFormatter formateador = new DataFormatter();
        List<FilaExcel> filas = new ArrayList<>();
        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet hoja = wb.getSheetAt(0);
            for (Row fila : hoja) {
                if (fila.getRowNum() == 0) continue;
                String[] valores = new String[columnas];
                boolean vacia = true;
                for (int i = 0; i < columnas; i++) {
                    Cell celda = fila.getCell(i);
                    valores[i] = celda == null ? "" : formateador.formatCellValue(celda).trim();
                    if (!valores[i].isEmpty()) vacia = false;
                }
                if (vacia) continue;
                if (filas.size() == MAX_FILAS_CARGA_MASIVA) {
                    throw new RuntimeException("El archivo supera el máximo de " + MAX_FILAS_CARGA_MASIVA
                            + " filas por carga. Divídelo en varios archivos.");
                }
                filas.add(new FilaExcel(fila.getRowNum() + 1, valores));
            }
        } catch (IOException | org.apache.poi.UnsupportedFileFormatException
                 | org.apache.poi.EncryptedDocumentException e) {
            log.error("No se pudo leer el Excel de carga masiva", e);
            throw new RuntimeException("No se pudo leer el archivo. Verifica que sea un Excel (.xlsx) válido y sin contraseña.");
        }
        return filas;
    }

    // Las anotaciones @NotBlank/@Email/@Min de los request normalmente las aplica
    // @Valid en el controller; aqui el request se arma a mano desde el Excel, asi
    // que se validan explicitamente con las mismas reglas.
    private <T> void validarBean(T bean) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(bean);
        if (!violaciones.isEmpty()) {
            ConstraintViolation<T> v = violaciones.iterator().next();
            throw new RuntimeException(v.getPropertyPath() + ": " + v.getMessage());
        }
    }

    private byte[] generarPlantillaExcel(String hoja, String[] cabeceras, String[] ejemplo, String nota) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new Color(0x1F, 0x7A, 0x63), wb.getStylesSource().getIndexedColors()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(Color.WHITE, wb.getStylesSource().getIndexedColors()));
            headerStyle.setFont(headerFont);

            // La hoja de datos solo trae la cabecera: el ejemplo va aparte para que
            // nadie lo suba por accidente como si fuera un registro real.
            XSSFSheet datos = wb.createSheet(hoja);
            escribirFila(datos.createRow(0), cabeceras, headerStyle);
            ajustarAnchos(datos, cabeceras.length);

            XSSFSheet ayuda = wb.createSheet("Instrucciones");
            escribirFila(ayuda.createRow(0), cabeceras, headerStyle);
            escribirFila(ayuda.createRow(1), ejemplo, null);
            ajustarAnchos(ayuda, cabeceras.length);
            ayuda.createRow(3).createCell(0).setCellValue(
                    "Diligencia la hoja \"" + hoja + "\": un registro por fila debajo de la cabecera, sin modificarla. Arriba hay un ejemplo.");
            ayuda.createRow(4).createCell(0).setCellValue(nota);
            ayuda.createRow(5).createCell(0).setCellValue(
                    "Cada fila se crea por separado: las que tengan error se reportan y las demás sí se guardan. Máximo "
                            + MAX_FILAS_CARGA_MASIVA + " filas por archivo.");

            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generando plantilla de carga masiva", e);
            throw new RuntimeException("No se pudo generar la plantilla. Intenta de nuevo.");
        }
    }

    private static void escribirFila(XSSFRow row, String[] valores, XSSFCellStyle estilo) {
        for (int i = 0; i < valores.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(valores[i]);
            if (estilo != null) cell.setCellStyle(estilo);
        }
    }

    private static void ajustarAnchos(XSSFSheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 512, 4000));
        }
    }

    // ===================== VALIDACIONES =====================

    /**
     * Valida que la cedula no este en uso por otro docente, estudiante o paciente.
     * Si se esta editando, pasar idActual (y tipo) para excluir ese registro.
     */
    /**
     * Normaliza un correo opcional: recorta, pasa a minúsculas y trata la cadena
     * vacía como ausencia de correo (el formulario envía "" cuando el campo se
     * deja en blanco, y eso debe guardarse como NULL para no chocar con el índice
     * único de {@code paciente.correo}).
     */
    private static String normalizarCorreoOpcional(String correo) {
        String normalizado = ValidacionesUsuario.normalizarCorreo(correo);
        return (normalizado == null || normalizado.isBlank()) ? null : normalizado;
    }

    /**
     * Devuelve la cuenta de acceso del paciente o falla con un mensaje claro si no
     * tiene. Los pacientes registrados presencialmente no tienen cuenta, así que
     * activarla, desactivarla o cambiarle la contraseña no significa nada.
     */
    private static Usuario requireCuenta(Paciente paciente, String accion) {
        Usuario cuenta = paciente.getUsuario();
        if (cuenta == null) {
            throw new EstadoInvalidoException("Este paciente no tiene cuenta de acceso (fue registrado sin correo), "
                    + "así que no se puede " + accion + ". Asígnale un correo primero si necesita ingresar al sistema.");
        }
        return cuenta;
    }

    private void validarCedulaUnica(String cedula, String tipoActual, Integer idActual) {
        // Validar unicidad SOLO dentro del mismo rol, no globalmente
        if ("DOCENTE".equals(tipoActual)) {
            boolean existe = idActual != null
                    ? docenteRepository.existsByCedulaAndIdDocenteNot(cedula, idActual)
                    : docenteRepository.existsByCedula(cedula);
            if (existe) {
                throw new DocumentoDuplicadoException("La cédula " + cedula + " ya está registrada en otro docente");
            }
        } else if ("ESTUDIANTE".equals(tipoActual)) {
            boolean existe = idActual != null
                    ? estudianteRepository.existsByCedulaAndIdEstudianteNot(cedula, idActual)
                    : estudianteRepository.existsByCedula(cedula);
            if (existe) {
                throw new DocumentoDuplicadoException("La cédula " + cedula + " ya está registrada en otro estudiante");
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
