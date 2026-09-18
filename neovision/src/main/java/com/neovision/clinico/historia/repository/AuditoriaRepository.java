package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByIdPacienteOrderByFechaHoraDesc(Integer idPaciente);

    /** Último registro de la cadena, para encadenar el hash del nuevo evento. */
    Optional<Auditoria> findTopByOrderByIdAuditoriaDesc();

    /** Toda la cadena en orden de creación, para verificar su integridad de extremo a extremo. */
    List<Auditoria> findAllByOrderByIdAuditoriaAsc();
}
