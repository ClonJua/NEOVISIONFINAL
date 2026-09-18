package com.neovision.usuario.config;

import com.neovision.usuario.entity.Rol;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.RolRepository;
import com.neovision.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Garantiza los roles del sistema y el primer administrador en una base de
 * datos vacía. El admin solo se crea si no existe ninguno y ADMIN_PASSWORD
 * está definida: nunca se siembra con una contraseña por defecto.
 */
@Component
@Order(1)
public class BootstrapSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSeeder.class);
    private static final List<String> ROLES =
            List.of("ROLE_ADMIN", "ROLE_DOCENTE", "ROLE_ESTUDIANTE", "ROLE_PACIENTE");

    private final RolRepository rolRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapSeeder(RolRepository rolRepo, UsuarioRepository usuarioRepo, PasswordEncoder encoder,
                           @Value("${app.bootstrap.admin.email:admin@neovision.com}") String adminEmail,
                           @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        for (String nombre : ROLES) {
            rolRepo.findByNombreRol(nombre).orElseGet(() -> {
                Rol r = new Rol();
                r.setNombreRol(nombre);
                log.info("BootstrapSeeder: creando rol {}", nombre);
                return rolRepo.save(r);
            });
        }

        if (usuarioRepo.existsByRol_NombreRol("ROLE_ADMIN")) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("BootstrapSeeder: no existe ningún administrador y ADMIN_PASSWORD no está definida; "
                    + "no se creó el admin inicial.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setCorreo(adminEmail);
        admin.setContrasena(encoder.encode(adminPassword));
        admin.setEstado(true);
        admin.setRol(rolRepo.findByNombreRol("ROLE_ADMIN").orElseThrow());
        usuarioRepo.save(admin);
        log.info("BootstrapSeeder: creado administrador inicial {}", adminEmail);
    }
}
