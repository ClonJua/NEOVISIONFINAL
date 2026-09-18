package com.neovision.common.validation;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validaciones de datos de usuario reutilizadas tanto en el autorregistro de
 * pacientes ({@code AuthServiceImpl}) como en la creación/edición de
 * docentes, estudiantes y pacientes desde el panel de administración
 * ({@code AdminServiceImpl}). Antes estaban duplicadas en ambas clases y ya
 * habían divergido (el chequeo de emojis solo existía en una de las dos
 * copias); centralizarlas evita que vuelvan a desincronizarse.
 */
@Component
public class ValidacionesUsuario {

    public void validarTelefono(String telefono) {
        if (telefono == null || !telefono.matches("^3\\d{9}$")) {
            throw new RuntimeException("El teléfono debe ser un celular colombiano válido (10 dígitos, inicia con 3)");
        }
    }

    public void validarTipoDocumentoEdad(String tipoDoc, LocalDate nacimiento) {
        int edad = Period.between(nacimiento, LocalDate.now()).getYears();
        switch (tipoDoc) {
            case "CC" -> {
                if (edad < 18) throw new RuntimeException("La cédula de ciudadanía solo aplica para mayores de 18 años");
            }
            case "TI" -> {
                if (edad < 7 || edad > 17) throw new RuntimeException("La tarjeta de identidad aplica para personas entre 7 y 17 años");
            }
            case "RC" -> {
                if (edad >= 7) throw new RuntimeException("El registro civil aplica para menores de 7 años");
            }
            case "CE" -> {
                if (edad < 18) throw new RuntimeException("La cédula de extranjería solo aplica para mayores de 18 años");
            }
            case "PA" -> { /* Pasaporte: cualquier edad */ }
            default -> throw new RuntimeException("Tipo de documento no válido. Usa CC, TI, RC, CE o PA");
        }
    }

    public void validarTextoSeguro(String texto, String campo) {
        if (texto == null) return;
        if (texto.matches(".*[<>{}\\[\\]\\\\].*")) {
            throw new RuntimeException("El campo " + campo + " contiene caracteres no permitidos");
        }
        if (texto.matches(".*[\\x{1F600}-\\x{1F9FF}\\x{2600}-\\x{27BF}\\x{1F300}-\\x{1F5FF}].*")) {
            throw new RuntimeException("El campo " + campo + " no puede contener emojis");
        }
    }

    /** Bloquea emojis y caracteres de control en la contraseña (la longitud mínima ya la valida @Size en el DTO). */
    public void validarContrasena(String contrasena) {
        if (contrasena == null) return;
        if (contrasena.matches(".*[\\x{1F600}-\\x{1F9FF}\\x{2600}-\\x{27BF}\\x{1F300}-\\x{1F5FF}].*")) {
            throw new RuntimeException("La contraseña no puede contener emojis");
        }
        if (contrasena.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*")) {
            throw new RuntimeException("La contraseña contiene caracteres no permitidos");
        }
    }

    /** Exige que el correo sea institucional (@unbosque.edu.co). Aplica a docentes y estudiantes. */
    public void validarCorreoInstitucional(String correo, String rol) {
        if (correo == null || !correo.toLowerCase().endsWith("@unbosque.edu.co")) {
            throw new RuntimeException("El correo del " + rol + " debe ser institucional (@unbosque.edu.co)");
        }
    }

    /**
     * Exige los datos del representante legal cuando el paciente es menor de edad
     * (Tarjeta de Identidad o Registro Civil). El consentimiento de tratamiento de
     * datos de un menor lo debe dar su representante legal, no el menor (Ley 1581
     * de 2012) — por eso estos datos quedan guardados junto con el paciente.
     */
    public void validarRepresentanteLegalSiMenor(String tipoDoc, String repNombre, String repDocumento, String repParentesco) {
        boolean esMenor = "TI".equals(tipoDoc) || "RC".equals(tipoDoc);
        if (!esMenor) return;
        if (esVacio(repNombre) || esVacio(repDocumento) || esVacio(repParentesco)) {
            throw new RuntimeException(
                    "El paciente es menor de edad: se requiere nombre, documento y parentesco del representante legal");
        }
    }

    private boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
