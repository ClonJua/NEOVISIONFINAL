package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.Adenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdendaRepository extends JpaRepository<Adenda, Integer> {
    List<Adenda> findByAtencionIdAtencionOrderByFechaAsc(Integer idAtencion);
}
