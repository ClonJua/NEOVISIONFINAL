package com.neovision.admin.service;

import com.neovision.admin.dto.CrearPacienteRequest;
import com.neovision.admin.dto.EditarPacienteAdminRequest;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EspecialidadRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.CorreoYaRegistradoException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.validation.ValidacionesUsuario;
import com.neovision.usuario.entity.Rol;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.RolRepository;
import com.neovision.usuario.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un paciente es un sujeto de atención clínica, no necesariamente un usuario del
 * sistema: la secretaría registra adultos mayores que no tienen correo, y esos
 * pacientes no deben quedar con cuenta de acceso.
 *
 * <p>Además, el dominio institucional queda reservado al rol académico: si un
 * estudiante abriera su cuenta de paciente con él, ese correo quedaría ocupado
 * ({@code usuario.correo} es único global) y después no se le podría crear la
 * cuenta de estudiante.
 */
class AdminServicePacienteSinCuentaTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private Validator validator;
    @Mock private AdminService self;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ValidacionesUsuario real: justamente es la lógica que se está probando.
        service = new AdminServiceImpl(usuarioRepository, rolRepository, docenteRepository,
                estudianteRepository, pacienteRepository, passwordEncoder, especialidadRepository,
                new ValidacionesUsuario(), validator, self);

        Rol rolPaciente = new Rol();
        rolPaciente.setIdRol(4);
        rolPaciente.setNombreRol("ROLE_PACIENTE");
        when(rolRepository.findByNombreRol("ROLE_PACIENTE")).thenReturn(Optional.of(rolPaciente));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CrearPacienteRequest requestBase() {
        CrearPacienteRequest r = new CrearPacienteRequest();
        r.setNombre("Rosa");
        r.setApellido("Medina");
        r.setTipoDocumento("CC");
        r.setNumeroDocumento("41234567");
        r.setFechaNacimiento("1948-03-12");
        r.setTelefono("3001234567");
        r.setAutorizaTratamientoDatos(true);
        return r;
    }

    @Test
    void sinCorreo_noSeCreaCuentaDeAcceso() {
        CrearPacienteRequest r = requestBase();
        r.setCorreo(null);

        var response = service.crearPaciente(r);

        // El paciente existe, pero no hay usuario que crear ni contraseña que pedir.
        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(response.getCorreo()).isNull();
        // Sin cuenta no hay estado que consultar: se considera activo.
        assertThat(response.getActivo()).isTrue();
    }

    @Test
    void correoVacio_seTrataComoAusencia() {
        CrearPacienteRequest r = requestBase();
        r.setCorreo("   ");

        var response = service.crearPaciente(r);

        // La cadena vacía debe guardarse como NULL: si no, chocaría con el índice
        // único de paciente.correo en cuanto hubiera un segundo paciente sin correo.
        assertThat(response.getCorreo()).isNull();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void conCorreo_creaCuentaYNormalizaAMinusculas() {
        CrearPacienteRequest r = requestBase();
        r.setCorreo("  Rosa.Medina@Gmail.COM ");
        r.setContrasena("ClaveSegura2026");
        when(usuarioRepository.existsByCorreo("rosa.medina@gmail.com")).thenReturn(false);

        var response = service.crearPaciente(r);

        verify(usuarioRepository).save(any(Usuario.class));
        assertThat(response.getCorreo()).isEqualTo("rosa.medina@gmail.com");
    }

    @Test
    void conCorreoPeroSinContrasena_seRechaza() {
        CrearPacienteRequest r = requestBase();
        r.setCorreo("rosa.medina@gmail.com");
        r.setContrasena(null);

        assertThatThrownBy(() -> service.crearPaciente(r))
                .hasMessageContaining("12 caracteres");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    void correoInstitucional_seRechaza() {
        CrearPacienteRequest r = requestBase();
        r.setCorreo("jnavarrop@unbosque.edu.co");
        r.setContrasena("ClaveSegura2026");

        assertThatThrownBy(() -> service.crearPaciente(r))
                .hasMessageContaining("reservado para docentes y estudiantes");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    void editar_pacienteConCuenta_noPuedeQuedarseSinCorreo() {
        Paciente paciente = pacienteConCuenta();
        when(pacienteRepository.findById(5)).thenReturn(Optional.of(paciente));

        EditarPacienteAdminRequest r = editarBase();
        r.setCorreo("");

        assertThatThrownBy(() -> service.editarPaciente(5, r))
                .hasMessageContaining("es su usuario de ingreso");
    }

    @Test
    void editar_correoNuevoYaRegistrado_seRechaza() {
        Paciente paciente = pacienteConCuenta();
        when(pacienteRepository.findById(5)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.existsByCorreo("otro@gmail.com")).thenReturn(true);

        EditarPacienteAdminRequest r = editarBase();
        r.setCorreo("otro@gmail.com");

        assertThatThrownBy(() -> service.editarPaciente(5, r))
                .isInstanceOf(CorreoYaRegistradoException.class);
    }

    @Test
    void editar_mismoCorreo_noChocaConsigoMismo() {
        Paciente paciente = pacienteConCuenta();
        when(pacienteRepository.findById(5)).thenReturn(Optional.of(paciente));
        // Su propio correo sí existe en usuario: no debe consultarse siquiera.
        when(usuarioRepository.existsByCorreo("rosa@gmail.com")).thenReturn(true);

        EditarPacienteAdminRequest r = editarBase();
        r.setCorreo("Rosa@Gmail.com");

        var response = service.editarPaciente(5, r);

        assertThat(response.getCorreo()).isEqualTo("rosa@gmail.com");
    }

    @Test
    void desactivar_pacienteSinCuenta_daMensajeClaro() {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(7);
        paciente.setUsuario(null);
        when(pacienteRepository.findById(7)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> service.desactivarPaciente(7))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("no tiene cuenta de acceso");
    }

    private Paciente pacienteConCuenta() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(30);
        usuario.setCorreo("rosa@gmail.com");
        usuario.setEstado(true);

        Paciente paciente = new Paciente();
        paciente.setIdPaciente(5);
        paciente.setCorreo("rosa@gmail.com");
        paciente.setFechaNacimiento(LocalDate.of(1948, 3, 12));
        paciente.setUsuario(usuario);
        return paciente;
    }

    private EditarPacienteAdminRequest editarBase() {
        EditarPacienteAdminRequest r = new EditarPacienteAdminRequest();
        r.setNombre("Rosa");
        r.setApellido("Medina");
        r.setTipoDocumento("CC");
        r.setNumeroDocumento("41234567");
        r.setFechaNacimiento("1948-03-12");
        r.setTelefono("3001234567");
        return r;
    }
}
