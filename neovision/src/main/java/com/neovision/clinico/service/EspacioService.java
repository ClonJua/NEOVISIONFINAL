package com.neovision.clinico.service;

import com.neovision.clinico.dto.EspacioRequest;
import com.neovision.clinico.dto.EspacioResponse;
import com.neovision.clinico.dto.SalonRequest;
import com.neovision.clinico.dto.SalonResponse;

import java.util.List;

/**
 * Servicio de gestión de espacios clínicos y sus salones.
 * Un espacio agrupa salones (ej. Clínica de Optometría → Cubículos 1-4).
 */
public interface EspacioService {

    /**
     * Crea un nuevo espacio clínico.
     *
     * @param request datos del espacio (nombre, descripción)
     * @return espacio creado
     */
    EspacioResponse crearEspacio(EspacioRequest request);

    /**
     * Retorna todos los espacios con sus salones.
     *
     * @return lista de espacios
     */
    List<EspacioResponse> listarEspacios();

    /**
     * Actualiza los datos de un espacio clínico.
     *
     * @param id      ID del espacio
     * @param request nuevos datos
     * @return espacio actualizado
     */
    EspacioResponse editarEspacio(Integer id, EspacioRequest request);

    /**
     * Desactiva un espacio sin eliminarlo de la base de datos.
     *
     * @param id ID del espacio
     */
    void desactivarEspacio(Integer id);

    /**
     * Reactiva un espacio previamente desactivado.
     *
     * @param id ID del espacio
     */
    void activarEspacio(Integer id);

    /**
     * Elimina permanentemente un espacio de la base de datos.
     *
     * @param id ID del espacio
     */
    void eliminarEspacio(Integer id);

    /**
     * Crea un nuevo salón dentro de un espacio clínico.
     *
     * @param request datos del salón (nombre, idEspacio)
     * @return salón creado
     */
    SalonResponse crearSalon(SalonRequest request);

    /**
     * Actualiza los datos de un salón.
     *
     * @param id      ID del salón
     * @param request nuevos datos
     * @return salón actualizado
     */
    SalonResponse editarSalon(Integer id, SalonRequest request);

    /**
     * Retorna los salones pertenecientes a un espacio específico.
     *
     * @param idEspacio ID del espacio
     * @return lista de salones del espacio
     */
    List<SalonResponse> listarSalonesPorEspacio(Integer idEspacio);

    /**
     * Desactiva un salón sin eliminarlo de la base de datos.
     *
     * @param id ID del salón
     */
    void desactivarSalon(Integer id);

    /**
     * Reactiva un salón previamente desactivado.
     *
     * @param id ID del salón
     */
    void activarSalon(Integer id);

    /**
     * Elimina permanentemente un salón de la base de datos.
     *
     * @param id ID del salón
     */
    void eliminarSalon(Integer id);
}
