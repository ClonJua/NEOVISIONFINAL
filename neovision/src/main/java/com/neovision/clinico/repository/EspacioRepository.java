package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Integer> {
    List<Espacio> findByActivoTrue();
}
