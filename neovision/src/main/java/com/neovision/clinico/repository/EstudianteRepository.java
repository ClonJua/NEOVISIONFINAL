package com.neovision.clinico.repository;

import com.neovision.clinico.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {
    Optional<Estudiante> findByUsuarioIdUsuario(Integer idUsuario);
    Optional<Estudiante> findByCorreo(String correo);
    boolean existsByCedula(String cedula);
    boolean existsByCedulaAndIdEstudianteNot(String cedula, Integer idEstudiante);

    // Candidatos a avance de semestre: solo estudiantes activos en 7 u 8 (9 es
    // terminal, no hay semestre 10 en este sistema).
    @Query("SELECT e FROM Estudiante e WHERE e.semestre IN :semestres AND e.usuario.estado = true")
    List<Estudiante> findActivosPorSemestre(@Param("semestres") List<Integer> semestres);
}
