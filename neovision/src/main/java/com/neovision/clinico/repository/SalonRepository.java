package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, Integer> {
    List<Salon> findByEspacioIdEspacio(Integer idEspacio);
    List<Salon> findByEspacioIdEspacioAndActivoTrue(Integer idEspacio);
}
