package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {

    Optional<Especialidad> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Especialidad> findByActivoTrueOrderByNombreAsc();

    List<Especialidad> findByEsDefaultTrue();
}
