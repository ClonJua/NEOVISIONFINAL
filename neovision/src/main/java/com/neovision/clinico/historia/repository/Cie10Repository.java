package com.neovision.clinico.historia.repository;

import com.neovision.clinico.historia.entity.Cie10;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Cie10Repository extends JpaRepository<Cie10, String> {

    /** Búsqueda por código o descripción (autocompletar). Solo códigos activos. */
    @Query("""
            SELECT c FROM Cie10 c
            WHERE c.activo = true AND (
                  LOWER(c.codigo) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY c.codigo
            """)
    List<Cie10> buscar(@Param("q") String q, Pageable pageable);

    boolean existsByCodigoAndActivoTrue(String codigo);
}
