package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.SolicitudHistoria;
import com.neovision.clinico.historia.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudHistoriaRepository extends JpaRepository<SolicitudHistoria, Integer> {
    List<SolicitudHistoria> findByPacienteIdPacienteOrderByFechaSolicitudDesc(Integer idPaciente);
    List<SolicitudHistoria> findAllByOrderByFechaSolicitudDesc();
    boolean existsByPacienteIdPacienteAndEstado(Integer idPaciente, EstadoSolicitud estado);
}
