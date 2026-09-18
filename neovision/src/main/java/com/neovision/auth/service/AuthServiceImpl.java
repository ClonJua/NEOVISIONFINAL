package com.neovision.auth.service;

import com.neovision.auth.dto.AuthResponse;
import com.neovision.auth.dto.LoginRequest;
import com.neovision.auth.dto.MicrosoftLoginRequest;
import com.neovision.auth.dto.RegistroRequest;
import com.neovision.auth.jwt.JwtService;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.AutenticacionMicrosoftException;
import com.neovision.common.exception.CorreoYaRegistradoException;
import com.neovision.common.exception.CredencialesInvalidasException;
import com.neovision.common.exception.RolNoEncontradoException;
import com.neovision.common.exception.UsuarioInactivoException;
import com.neovision.common.exception.UsuarioNoRegistradoException;
import com.neovision.common.validation.ValidacionesUsuario;
import com.neovision.usuario.entity.Rol;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.RolRepository;
import com.neovision.usuario.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Schema(description = "hace la logica de negocio de autenticacion, como el login, registro y login con Microsoft.")
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PacienteRepository pacienteRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MicrosoftTokenService microsoftTokenService;
    private final ValidacionesUsuario validaciones;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PacienteRepository pacienteRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           MicrosoftTokenService microsoftTokenService,
                           ValidacionesUsuario validaciones,
                           LoginAttemptService loginAttemptService,
                           TokenBlacklistService tokenBlacklistService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.pacienteRepository = pacienteRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.microsoftTokenService = microsoftTokenService;
        this.validaciones = validaciones;
        this.loginAttemptService = loginAttemptService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        loginAttemptService.verificarNoBloqueado(request.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> {
                    loginAttemptService.registrarFallo(request.getCorreo());
                    return new CredencialesInvalidasException("Correo o contraseña inválidos");
                });


        if (usuario.getContrasena() == null) {
            loginAttemptService.registrarFallo(request.getCorreo());
            throw new CredencialesInvalidasException("Correo o contraseña inválidos");
        }

        if (!passwordEncoder.matches(request.getContrasena(), usuario.getContrasena())) {
            loginAttemptService.registrarFallo(request.getCorreo());
            throw new CredencialesInvalidasException("Correo o contraseña inválidos");
        }

        if (!usuario.getEstado()) {
            throw new UsuarioInactivoException("Tu usuario ha sido desactivado. Contacta al administrador");
        }

        loginAttemptService.registrarExito(request.getCorreo());
        String token = jwtService.generateToken(usuario.getCorreo(), usuario.getRol().getNombreRol());
        return new AuthResponse(token, usuario.getRol().getNombreRol(), usuario.getCorreo());
    }

    @Override
    public AuthResponse registro(RegistroRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoYaRegistradoException("El correo " + request.getCorreo() + " ya está registrado");
        }

        // Validar telefono colombiano (10 digitos, empieza por 3)
        validaciones.validarTelefono(request.getTelefono());

        // Validar tipo de documento vs edad
        LocalDate nacimiento = LocalDate.parse(request.getFechaNacimiento());
        validaciones.validarTipoDocumentoEdad(request.getTipoDocumento(), nacimiento);

        // Validar que no tenga caracteres peligrosos
        validaciones.validarTextoSeguro(request.getNombre(), "nombre");
        validaciones.validarTextoSeguro(request.getApellido(), "apellido");
        validaciones.validarContrasena(request.getContrasena());

        // Si el paciente es menor de edad (TI o RC), el consentimiento lo debe dar
        // su representante legal (Ley 1581/2012), no el menor.
        validaciones.validarRepresentanteLegalSiMenor(request.getTipoDocumento(),
                request.getRepresentanteNombre(), request.getRepresentanteDocumento(), request.getRepresentanteParentesco());

        // Consentimiento de tratamiento de datos obligatorio (Ley 1581/2012)
        if (!Boolean.TRUE.equals(request.getAutorizaTratamientoDatos())) {
            throw new RuntimeException("Debes autorizar el tratamiento de tus datos personales para registrarte");
        }

        Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                .orElseThrow(() -> new RolNoEncontradoException("Rol PACIENTE no encontrado en el sistema"));

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
        paciente.setAutorizaTratamientoDatos(true);
        paciente.setFechaAutorizacionDatos(java.time.LocalDateTime.now());
        paciente.setRepresentanteNombre(request.getRepresentanteNombre());
        paciente.setRepresentanteDocumento(request.getRepresentanteDocumento());
        paciente.setRepresentanteParentesco(request.getRepresentanteParentesco());
        pacienteRepository.save(paciente);

        String token = jwtService.generateToken(usuario.getCorreo(), rolPaciente.getNombreRol());
        return new AuthResponse(token, rolPaciente.getNombreRol(), usuario.getCorreo());
    }

    @Override
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request) {
        String correo = microsoftTokenService.extractEmail(request.getAccessToken());

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoRegistradoException("No estás registrado en el sistema. Contacta a la secretaría"));

        if (!usuario.getEstado()) {
            throw new UsuarioInactivoException("Tu usuario ha sido desactivado. Contacta al administrador");
        }

        String rol = usuario.getRol().getNombreRol();
        if (rol.equals("ROLE_PACIENTE")) {
            throw new AutenticacionMicrosoftException("Los pacientes deben iniciar sesión con correo y contraseña");
        }

        String token = jwtService.generateToken(usuario.getCorreo(), rol);
        return new AuthResponse(token, rol, usuario.getCorreo());
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        try {
            String jti = jwtService.extractJti(token);
            java.time.Instant expiraEn = jwtService.extractExpiration(token).toInstant();
            tokenBlacklistService.revocar(jti, expiraEn);
        } catch (Exception e) {
            // Token ya invalido/expirado: no hay nada que revocar.
        }
    }
}
