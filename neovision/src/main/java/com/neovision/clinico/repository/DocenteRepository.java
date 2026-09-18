package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Docente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {
    Optional<Docente> findByUsuarioIdUsuario(Integer idUsuario);
    Optional<Docente> findByCorreo(String correo);
    boolean existsByCedula(String cedula);
    boolean existsByCedulaAndIdDocenteNot(String cedula, Integer idDocente);

    /**
     * Carga el docente junto con sus especialidades en una sola consulta.
     * Necesario para acceder a la coleccion fuera del scope transaccional con OSIV deshabilitado.
     */
    @EntityGraph(attributePaths = {"especialidades"})
    Optional<Docente> findWithEspecialidadesByUsuarioIdUsuario(Integer idUsuario);
}
