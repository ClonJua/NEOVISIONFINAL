package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Integer> {

    String FETCH_GRAPH = "cita-full";

    @Override
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findAll();

    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findByEstado(String estado);

    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    Page<Cita> findAll(Pageable pageable);

    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    Page<Cita> findByEstado(String estado, Pageable pageable);

    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findBySesionIdSesion(Integer idSesion);

    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findByPacienteIdPaciente(Integer idPaciente);

    // Una cita puede tener hasta dos estudiantes (atienden en pareja). Todas las
    // consultas "por estudiante" consideran ambos puestos del bloque.
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c LEFT JOIN c.estudiante2 e2
            WHERE c.estudiante.idEstudiante = :idEstudiante OR e2.idEstudiante = :idEstudiante
            """)
    List<Cita> findByEstudianteIdEstudiante(@Param("idEstudiante") Integer idEstudiante);

    // Vínculo asistencial (necesidad de saber) para autorizar la lectura de la
    // historia clínica: ¿este estudiante / docente ha tenido contacto con el paciente?
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Cita c LEFT JOIN c.estudiante2 e2
            WHERE c.paciente.idPaciente = :idPaciente
              AND (c.estudiante.idEstudiante = :idEstudiante OR e2.idEstudiante = :idEstudiante)
            """)
    boolean existsByEstudianteIdEstudianteAndPacienteIdPaciente(@Param("idEstudiante") Integer idEstudiante,
                                                                 @Param("idPaciente") Integer idPaciente);

    // Trae las citas para verificar la ventana horaria de acceso del estudiante
    // (HistoriaAccessGuard): no basta con que exista vínculo alguna vez, debe
    // haber una cita cuyo horario cubra el momento actual (con margen).
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c LEFT JOIN c.estudiante2 e2
            WHERE c.paciente.idPaciente = :idPaciente
              AND (c.estudiante.idEstudiante = :idEstudiante OR e2.idEstudiante = :idEstudiante)
            """)
    List<Cita> findByEstudianteIdEstudianteAndPacienteIdPaciente(@Param("idEstudiante") Integer idEstudiante,
                                                                  @Param("idPaciente") Integer idPaciente);

    boolean existsBySesionDocenteIdDocenteAndPacienteIdPaciente(Integer idDocente, Integer idPaciente);

    // Un paciente solo puede tener UNA cita reservada a la vez. Como la cita ya
    // agrupa a los (hasta dos) estudiantes que lo atienden, no hace falta —ni se
    // permite— una segunda reserva para el mismo paciente.
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Cita c
            WHERE c.paciente.idPaciente = :idPaciente
              AND c.estado = :estado
              AND (:idCitaExcluir IS NULL OR c.idCita <> :idCitaExcluir)
            """)
    boolean existeCitaReservada(@Param("idPaciente") Integer idPaciente,
                                @Param("estado") String estado,
                                @Param("idCitaExcluir") Integer idCitaExcluir);

    // Bulk update para vencer citas pasadas sin cargar entidades en memoria.
    // Una sola UPDATE en vez de N SELECT + N UPDATE.
    @Modifying
    @Query("""
            UPDATE Cita c SET c.estado = 'VENCIDA'
            WHERE c.estado IN ('RESERVADO', 'DISPONIBLE')
              AND (c.sesion.fecha < :hoy
                   OR (c.sesion.fecha = :hoy AND c.sesion.horaFin <= :horaActual))
            """)
    int marcarVencidas(@Param("hoy") java.time.LocalDate hoy,
                       @Param("horaActual") java.time.LocalTime horaActual);

    // Query optimizada para bloques disponibles filtrando en BD (no en memoria).
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c
            WHERE c.estado = 'DISPONIBLE'
              AND c.sesion.activo = true
              AND c.sesion.fecha >= :hoy
            """)
    List<Cita> findBloquesDisponibles(@Param("hoy") java.time.LocalDate hoy);

    // Bloques disponibles filtrando por especialidad: el docente que supervisa la sesion
    // debe tener asignada esa especialidad (o ser una especialidad marcada como esDefault=true,
    // que todos los docentes pueden hacer).
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT DISTINCT c FROM Cita c
            JOIN c.sesion.docente d
            LEFT JOIN d.especialidades de
            WHERE c.estado = 'DISPONIBLE'
              AND c.sesion.activo = true
              AND c.sesion.fecha >= :hoy
              AND (
                de.idEspecialidad = :idEspecialidad
                OR EXISTS (SELECT 1 FROM Especialidad e2
                           WHERE e2.idEspecialidad = :idEspecialidad
                             AND e2.esDefault = true)
              )
            """)
    List<Cita> findBloquesDisponiblesPorEspecialidad(
            @Param("hoy") java.time.LocalDate hoy,
            @Param("idEspecialidad") Integer idEspecialidad);

    // Para validacion rapida de overlap: solo trae las citas activas de la sesion.
    @Query("""
            SELECT c FROM Cita c
            WHERE c.sesion.idSesion = :idSesion
              AND c.estado NOT IN ('VENCIDA', 'CANCELADO')
            """)
    List<Cita> findActivasBySesion(@Param("idSesion") Integer idSesion);

    // Conteo rapido para validacion de cupos sin cargar las citas.
    @Query("""
            SELECT COUNT(c) FROM Cita c
            WHERE c.sesion.idSesion = :idSesion
              AND c.estado IN ('DISPONIBLE', 'RESERVADO')
            """)
    long countActivasBySesion(@Param("idSesion") Integer idSesion);

    // Obtener citas de las sesiones de un docente
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c
            WHERE c.sesion.docente.idDocente = :idDocente
            ORDER BY c.sesion.fecha DESC, c.sesion.horaInicio DESC
            """)
    List<Cita> findByDocenteIdDocente(@Param("idDocente") Integer idDocente);

    // Contar cancelaciones del paciente en el último mes (para límite de cancelaciones)
    @Query("""
            SELECT COUNT(c) FROM Cita c
            WHERE c.paciente.idPaciente = :idPaciente
              AND c.estado = 'CANCELADO'
              AND c.fechaReserva >= :hace30Dias
            """)
    long countCancelacionesEnMes(@Param("idPaciente") Integer idPaciente,
                                  @Param("hace30Dias") java.time.LocalDateTime hace30Dias);

    // Encontrar citas RESERVADO que necesitan recordatorio en 24h (pero no enviado)
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c
            WHERE c.estado = 'RESERVADO'
              AND c.recordatorio24hEnviado = false
              AND c.sesion.fecha = :manana
              AND c.sesion.horaInicio IS NOT NULL
            """)
    List<Cita> findCitasRecordatorio24h(@Param("manana") java.time.LocalDate manana);

    // Encontrar citas RESERVADO que necesitan recordatorio en 1h (pero no enviado)
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            SELECT c FROM Cita c
            WHERE c.estado = 'RESERVADO'
              AND c.recordatorio1hEnviado = false
              AND c.sesion.fecha = :hoy
              AND c.sesion.horaInicio IS NOT NULL
              AND c.sesion.horaInicio <= :ahora1h
              AND c.sesion.horaInicio > :ahoraMenos1h
            """)
    List<Cita> findCitasRecordatorio1h(@Param("hoy") java.time.LocalDate hoy,
                                       @Param("ahora1h") java.time.LocalTime ahora1h,
                                       @Param("ahoraMenos1h") java.time.LocalTime ahoraMenes1h);

    // Contar citas COMPLETADO de un estudiante (para progreso semestral). Cuenta
    // igual si el estudiante fue el primero o el segundo del bloque: ambos atendieron.
    @Query("""
            SELECT COUNT(c) FROM Cita c LEFT JOIN c.estudiante2 e2
            WHERE (c.estudiante.idEstudiante = :idEstudiante OR e2.idEstudiante = :idEstudiante)
              AND c.estado = 'COMPLETADO'
            """)
    long countCompletadasByEstudiante(@Param("idEstudiante") Integer idEstudiante);

    // Validar conflictos de horario del estudiante (en cualquiera de los dos
    // puestos del bloque): citas DISPONIBLE o RESERVADO en la misma fecha/hora.
    @Query("""
            SELECT c FROM Cita c LEFT JOIN c.estudiante2 e2
            WHERE (c.estudiante.idEstudiante = :idEstudiante OR e2.idEstudiante = :idEstudiante)
              AND c.sesion.fecha = :fecha
              AND c.estado IN ('DISPONIBLE', 'RESERVADO')
              AND c.sesion.horaInicio IS NOT NULL
              AND c.sesion.horaFin IS NOT NULL
              AND (
                (c.sesion.horaInicio < :horaFin AND c.sesion.horaFin > :horaInicio)
              )
            """)
    List<Cita> findCitasConflicto(@Param("idEstudiante") Integer idEstudiante,
                                   @Param("fecha") java.time.LocalDate fecha,
                                   @Param("horaInicio") java.time.LocalTime horaInicio,
                                   @Param("horaFin") java.time.LocalTime horaFin);

    // ===================== REPORTES (agregados en BD, no en memoria) =====================

    // Conteo por estado: siempre a lo sumo 5 filas (DISPONIBLE/RESERVADO/COMPLETADO/
    // CANCELADO/VENCIDA), independiente de cuantas citas existan historicamente.
    @Query("SELECT c.estado, COUNT(c) FROM Cita c GROUP BY c.estado")
    List<Object[]> conteoPorEstado();

    // Completadas por estudiante: a lo sumo una fila por estudiante, no por cita.
    // Se cuentan por separado el primer y el segundo puesto del bloque y el
    // servicio las suma (JPQL no tiene UNION).
    @Query("SELECT c.estudiante.idEstudiante, COUNT(c) FROM Cita c WHERE c.estado = 'COMPLETADO' GROUP BY c.estudiante.idEstudiante")
    List<Object[]> conteoCompletadasPorEstudiante();

    @Query("SELECT c.estudiante2.idEstudiante, COUNT(c) FROM Cita c WHERE c.estado = 'COMPLETADO' AND c.estudiante2 IS NOT NULL GROUP BY c.estudiante2.idEstudiante")
    List<Object[]> conteoCompletadasPorEstudiante2();

    @Query("SELECT COUNT(DISTINCT c.sesion.salon.idSalon) FROM Cita c WHERE c.sesion.salon IS NOT NULL")
    long countDistinctSalonesUsados();

    // Estudiantes con al menos una cita, en cualquiera de los dos puestos del bloque.
    @Query("""
            SELECT COUNT(e) FROM Estudiante e
            WHERE EXISTS (SELECT c.idCita FROM Cita c WHERE c.estudiante = e OR c.estudiante2 = e)
            """)
    long countDistinctEstudiantesConCita();

    // Ocupacion por periodo: solo la ventana reciente, no el historico completo.
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findBySesionFechaGreaterThanEqualOrderBySesionFechaAsc(java.time.LocalDate desde);

    // Inasistencias: filtrado en BD a solo los estados relevantes.
    @EntityGraph(value = FETCH_GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    List<Cita> findByEstadoInOrderBySesionFechaDesc(List<String> estados);
}
