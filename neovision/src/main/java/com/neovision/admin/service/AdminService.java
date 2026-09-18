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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Servicio de administración de usuarios del sistema.
 * Gestiona el ciclo de vida de docentes, estudiantes y pacientes:
 * creación, edición, desactivación, reactivación y eliminación.
 */
@Schema(description = "Crea la logica de negocio, como *Crud*.")
public interface AdminService {

    /**
     * Registra un nuevo docente en el sistema.
     *
     * @param request datos del docente a crear
     * @return datos del docente creado
     */
    DocenteResponse crearDocente(CrearDocenteRequest request);

    /**
     * Actualiza los datos básicos de un docente .
     *
     * @param id      ID del docente
     * @param request nuevos datos
     * @return datos actualizados del docente
     */
    DocenteResponse editarDocente(Integer id, EditarDocenteRequest request);

    /**
     * Actualiza los datos de un docente incluyendo cédula y correo (solo admin).
     *
     * @param id      ID del docente
     * @param request nuevos datos con cédula y correo
     * @return datos actualizados del docente
     */
    DocenteResponse editarDocente(Integer id, EditarDocenteAdminRequest request);

    /**
     * Retorna la lista de todos los docentes registrados.
     *
     * @return lista de docentes
     */
    List<DocenteResponse> listarDocentes();

    /**
     * Retorna un docente por su ID.
     *
     * @param id ID del docente
     * @return datos del docente
     */
    DocenteResponse obtenerDocente(Integer id);

    /**
     * Desactiva un docente sin eliminarlo de la base de datos.
     *
     * @param id ID del docente
     */
    void desactivarDocente(Integer id);

    /**
     * Elimina un docente permanentemente de la base de datos.
     *
     * @param id ID del docente
     */
    void eliminarDocente(Integer id);

    /**
     * Registra un nuevo estudiante en el sistema.
     *
     * @param request datos del estudiante a crear
     * @return datos del estudiante creado
     */
    EstudianteResponse crearEstudiante(CrearEstudianteRequest request);

    /**
     * Actualiza los datos básicos de un estudiante .
     *
     * @param id      ID del estudiante
     * @param request nuevos datos
     * @return datos actualizados del estudiante
     */
    EstudianteResponse editarEstudiante(Integer id, EditarEstudianteRequest request);

    /**
     * Actualiza los datos de un estudiante incluyendo cédula y correo (solo admin).
     *
     * @param id      ID del estudiante
     * @param request nuevos datos con cédula y correo
     * @return datos actualizados del estudiante
     */
    EstudianteResponse editarEstudiante(Integer id, EditarEstudianteAdminRequest request);

    /**
     * Retorna la lista de todos los estudiantes registrados.
     *
     * @return lista de estudiantes
     */
    List<EstudianteResponse> listarEstudiantes();

    /**
     * Retorna un estudiante por su ID.
     *
     * @param id ID del estudiante
     * @return datos del estudiante
     */
    EstudianteResponse obtenerEstudiante(Integer id);

    /**
     * Desactiva un estudiante sin eliminarlo de la base de datos.
     *
     * @param id ID del estudiante
     */
    void desactivarEstudiante(Integer id);

    /**
     * Elimina un estudiante permanentemente de la base de datos.
     *
     * @param id ID del estudiante
     */
    void eliminarEstudiante(Integer id);

    /**
     * Registra un nuevo paciente en el sistema.
     *
     * @param request datos del paciente a crear
     * @return datos del paciente creado
     */
    PacienteResponse crearPaciente(CrearPacienteRequest request);

    /**
     * Actualiza los datos básicos de un paciente .
     *
     * @param id      ID del paciente
     * @param request nuevos datos
     * @return datos actualizados del paciente
     */
    PacienteResponse editarPaciente(Integer id, EditarPacienteRequest request);

    /**
     * Actualiza los datos de un paciente incluyendo documento y correo (solo admin).
     *
     * @param id      ID del paciente
     * @param request nuevos datos con documento y correo
     * @return datos actualizados del paciente
     */
    PacienteResponse editarPaciente(Integer id, EditarPacienteAdminRequest request);

    /**
     * Retorna la lista de todos los pacientes registrados.
     *
     * @return lista de pacientes
     */
    List<PacienteResponse> listarPacientes();

    /**
     * Retorna un paciente por su ID.
     *
     * @param id ID del paciente
     * @return datos del paciente
     */
    PacienteResponse obtenerPaciente(Integer id);

    /**
     * Desactiva un paciente sin eliminarlo de la base de datos.
     *
     * @param id ID del paciente
     */
    void desactivarPaciente(Integer id);
    void resetearContrasenaPaciente(Integer id, String nuevaContrasena);

    /**
     * Elimina un paciente permanentemente de la base de datos.
     *
     * @param id ID del paciente
     */
    void eliminarPaciente(Integer id);

    /**
     * Reactiva un docente previamente desactivado.
     *
     * @param id ID del docente
     */
    void reactivarDocente(Integer id);

    /**
     * Reactiva un estudiante previamente desactivado.
     *
     * @param id ID del estudiante
     */
    void reactivarEstudiante(Integer id);

    /**
     * Reactiva un paciente previamente desactivado.
     *
     * @param id ID del paciente
     */
    void reactivarPaciente(Integer id);
}
