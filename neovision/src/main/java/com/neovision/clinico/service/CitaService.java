package com.neovision.clinico.service;

import com.neovision.clinico.dto.CambiarEstadoCitaRequest;
import com.neovision.clinico.dto.CitaRequest;
import com.neovision.clinico.dto.CitaResponse;
import com.neovision.clinico.dto.DistribucionEstudianteResponse;
import com.neovision.clinico.dto.EliminarVencidasResponse;
import com.neovision.clinico.dto.IndicadoresResponse;
import com.neovision.clinico.dto.ProgresoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CitaService {

    // --- Admin/Secretaria ---
    CitaResponse crearBloque(CitaRequest request);
    List<CitaResponse> listarCitas(String estado);
    Page<CitaResponse> listarCitas(String estado, Pageable pageable);
    CitaResponse editarCita(Integer idCita, CitaRequest request);
    void eliminarCita(Integer idCita);
    EliminarVencidasResponse eliminarVencidas();
    CitaResponse cambiarEstado(Integer idCita, CambiarEstadoCitaRequest request);

    // --- Paciente ---
    List<CitaResponse> bloquesDisponibles();
    List<CitaResponse> bloquesDisponiblesPorEspecialidad(Integer idEspecialidad);
    CitaResponse reservarBloque(String correoPaciente, Integer idCita,
                                 Integer idEspecialidad, String subtipoControl);
    CitaResponse cancelarPaciente(String correoPaciente, Integer idCita, String observaciones);
    CitaResponse reprogramarPaciente(String correoPaciente, Integer idCitaActual, Integer idCitaNueva);
    List<CitaResponse> misCitas(String correoPaciente);

    // --- Estudiante ---
    List<CitaResponse> misBloques(String correoEstudiante);
    CitaResponse asignarPaciente(String correoEstudiante, Integer idCita, Integer idPaciente);
    ProgresoResponse progresoEstudiante(String correoEstudiante);
    CitaResponse cancelarEstudiante(String correoEstudiante, Integer idCita, String observaciones);
    CitaResponse reprogramarEstudiante(String correoEstudiante, Integer idCitaActual, Integer idCitaNueva);

    // --- Admin reprogramar ---
    CitaResponse reprogramarAdmin(Integer idCitaActual, Integer idCitaNueva);

    // --- Docente ---
    List<CitaResponse> misCitasDocente(Integer idDocente);
    CitaResponse confirmarAsistenciaDocente(Integer idDocente, Integer idCita);

    // --- Admin historial ---
    List<CitaResponse> historialPaciente(Integer idPaciente);

    // --- Admin reportes (agregados en BD, no cargan todas las citas a memoria) ---
    IndicadoresResponse indicadores();
    List<DistribucionEstudianteResponse> distribucionPorEstudiante();
    List<CitaResponse> ocupacionReciente();
    List<CitaResponse> inasistencias();

    // --- Admin: metas de pacientes por semestre (configurables) ---
    List<com.neovision.clinico.dto.MetaSemestreResponse> listarMetasSemestre();
    com.neovision.clinico.dto.MetaSemestreResponse actualizarMetaSemestre(Integer semestre, Integer minimoPacientes);

    // --- Admin: avance de semestre en lote (estudiantes que ya cumplieron su meta) ---
    List<com.neovision.clinico.dto.CandidatoAvanceSemestreResponse> listarCandidatosAvanceSemestre();
    int avanzarSemestre(List<Integer> idsEstudiantes);
}
