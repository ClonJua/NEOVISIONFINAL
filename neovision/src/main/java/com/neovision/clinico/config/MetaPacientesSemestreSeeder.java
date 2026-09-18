package com.neovision.clinico.config;

import com.neovision.clinico.entity.MetaPacientesSemestre;
import com.neovision.clinico.repository.MetaPacientesSemestreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga las metas iniciales de pacientes por semestre clinico (7, 8, 9).
 * Idempotente: solo inserta los semestres que falten; los valores existentes
 * no se sobrescriben, ya que el admin puede haberlos editado.
 */
@Component
@Order(12)
public class MetaPacientesSemestreSeeder implements CommandLineRunner {

    private final MetaPacientesSemestreRepository repository;

    public MetaPacientesSemestreSeeder(MetaPacientesSemestreRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        crearSiFalta(7, 35);
        crearSiFalta(8, 65);
        crearSiFalta(9, 85);
    }

    private void crearSiFalta(int semestre, int minimoPacientes) {
        if (repository.existsById(semestre)) return;
        repository.save(new MetaPacientesSemestre(semestre, minimoPacientes));
    }
}
