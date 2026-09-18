package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Integer> {
    Optional<HistoriaClinica> findByPacienteIdPaciente(Integer idPaciente);
    boolean existsByPacienteIdPaciente(Integer idPaciente);
}
