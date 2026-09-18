package com.neovision.clinico.service;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Estudiante;

/**
 * Helpers para tratar a los (hasta dos) estudiantes de una cita como una sola
 * unidad: en la practica clinica un paciente es atendido normalmente por una
 * pareja de estudiantes, y esa pareja comparte la cita, las notificaciones y la
 * historia clinica del encuentro.
 */
public final class CitaEstudiantes {

    private CitaEstudiantes() {}

    /** true si el estudiante es uno de los (uno o dos) estudiantes del bloque. */
    public static boolean incluye(Cita cita, Integer idEstudiante) {
        if (cita == null || idEstudiante == null) return false;
        return tieneId(cita.getEstudiante(), idEstudiante) || tieneId(cita.getEstudiante2(), idEstudiante);
    }

    /** true si el correo pertenece a uno de los estudiantes del bloque. */
    public static boolean incluyeCorreo(Cita cita, String correo) {
        if (cita == null || correo == null) return false;
        return tieneCorreo(cita.getEstudiante(), correo) || tieneCorreo(cita.getEstudiante2(), correo);
    }

    /** Correos de los estudiantes del bloque separados por coma, para un solo envio de correo. */
    public static String correos(Cita cita) {
        Estudiante e1 = cita.getEstudiante();
        Estudiante e2 = cita.getEstudiante2();
        if (e1 == null) return e2 != null ? e2.getCorreo() : null;
        return e2 == null ? e1.getCorreo() : e1.getCorreo() + "," + e2.getCorreo();
    }

    /** "Ana Perez" o "Ana Perez y Luis Gomez". */
    public static String nombres(Cita cita) {
        Estudiante e1 = cita.getEstudiante();
        Estudiante e2 = cita.getEstudiante2();
        if (e1 == null) return e2 != null ? nombre(e2) : "";
        return e2 == null ? nombre(e1) : nombre(e1) + " y " + nombre(e2);
    }

    private static String nombre(Estudiante e) {
        return e.getNombre() + " " + e.getApellido();
    }

    private static boolean tieneId(Estudiante e, Integer id) {
        return e != null && id.equals(e.getIdEstudiante());
    }

    private static boolean tieneCorreo(Estudiante e, String correo) {
        return e != null && e.getCorreo() != null && e.getCorreo().equalsIgnoreCase(correo);
    }
}
