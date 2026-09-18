package com.neovision.clinico.service;

import com.neovision.clinico.dto.SesionRequest;
import com.neovision.clinico.dto.SesionResponse;
import com.neovision.clinico.dto.CrearSesionRecurrenteRequest;
import com.neovision.clinico.dto.SesionRecurrenteResponse;

import java.util.List;

/**
 * Servicio de gestión de sesiones de práctica clínica.
 * Una sesión agrupa bloques de cita para una fecha, docente y salón.
 */
public interface SesionService {

    /**
     * Crea una nueva sesión de práctica con sus bloques.
     *
     * @param request datos de la sesión (fecha, docente, salón, cupos, estudiantes)
     * @return sesión creada
     */
    SesionResponse crearSesion(SesionRequest request);

    /**
     * Crea múltiples sesiones recurrentes en un rango de fechas para un día de la semana.
     *
     * @param request patrón de recurrencia (día, rango de fechas, horario, cupos)
     * @return resumen con el número de sesiones creadas
     */
    SesionRecurrenteResponse crearSesionesRecurrentes(CrearSesionRecurrenteRequest request);

    /**
     * Retorna todas las sesiones activas del sistema.
     *
     * @return lista de sesiones
     */
    List<SesionResponse> listarSesiones();

    /**
     * Retorna las sesiones que ocurren en una fecha específica.
     *
     * @param fecha fecha en formato YYYY-MM-DD
     * @return lista de sesiones en esa fecha
     */
    List<SesionResponse> listarSesionesPorFecha(String fecha);

    /**
     * Actualiza los datos de una sesión existente.
     *
     * @param id      ID de la sesión
     * @param request nuevos datos
     * @return sesión actualizada
     */
    SesionResponse editarSesion(Integer id, SesionRequest request);

    /**
     * Desactiva una sesión sin eliminarla de la base de datos.
     *
     * @param id ID de la sesión
     */
    void desactivarSesion(Integer id);

    /**
     * Elimina permanentemente una sesión y sus bloques de cita.
     *
     * @param id ID de la sesión
     */
    void eliminarSesion(Integer id);
}
