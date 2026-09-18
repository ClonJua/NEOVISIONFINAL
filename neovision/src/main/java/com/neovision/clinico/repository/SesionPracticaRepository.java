package com.neovision.clinico.repository;

import com.neovision.clinico.entity.SesionPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SesionPracticaRepository extends JpaRepository<SesionPractica, Integer> {
    List<SesionPractica> findByFechaAndActivoTrue(LocalDate fecha);
    List<SesionPractica> findByFechaBetweenAndActivoTrue(LocalDate desde, LocalDate hasta);
    List<SesionPractica> findByActivoTrue();
    List<SesionPractica> findBySalonIdSalonAndFechaAndActivoTrue(Integer idSalon, LocalDate fecha);
    List<SesionPractica> findByDocenteIdDocenteAndActivoTrue(Integer idDocente);

    /**
     * Carga las sesiones del docente con estudiantes, salon, espacio y docente
     * en una sola consulta. Necesario con OSIV deshabilitado para evitar
     * LazyInitializationException al mapear a DTO fuera de la transaccion.
     */
    @Query("""
            select distinct s from SesionPractica s
            left join fetch s.estudiantes
            left join fetch s.salon sa
            left join fetch sa.espacio
            left join fetch s.docente
            where s.docente.idDocente = :idDocente and s.activo = true
            """)
    List<SesionPractica> findByDocenteWithDetails(@Param("idDocente") Integer idDocente);
}
