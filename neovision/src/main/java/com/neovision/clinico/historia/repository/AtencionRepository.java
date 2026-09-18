package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.enums.EstadoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionRepository extends JpaRepository<Atencion, Integer> {

    Optional<Atencion> findByCitaIdCita(Integer idCita);

    List<Atencion> findByHistoriaClinicaIdHistoriaOrderByFechaAtencionDesc(Integer idHistoria);

    List<Atencion> findByEstudianteIdEstudianteOrderByFechaAtencionDesc(Integer idEstudiante);

    /** Bandeja de revisión del docente supervisor. */
    List<Atencion> findByDocenteSupervisorIdDocenteAndEstadoOrderByFechaEnvioRevisionAsc(
            Integer idDocente, EstadoAtencion estado);

    List<Atencion> findByDocenteSupervisorIdDocenteOrderByFechaAtencionDesc(Integer idDocente);

    boolean existsByCitaIdCita(Integer idCita);
}
