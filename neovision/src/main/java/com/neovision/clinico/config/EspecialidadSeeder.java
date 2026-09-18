package com.neovision.clinico.config;

import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Especialidad;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EspecialidadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed inicial del catálogo de especialidades.
 *
 * Why: requisito clínico — Funcional es la cita por defecto que todos los docentes pueden
 * supervisar; las demás especialidades restringen los slots disponibles para el paciente
 * según las que tenga asignadas el docente de la sesión.
 */
@Component
@Order(20)
public class EspecialidadSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EspecialidadSeeder.class);

    private final EspecialidadRepository especialidadRepository;
    private final DocenteRepository docenteRepository;

    public EspecialidadSeeder(EspecialidadRepository especialidadRepository,
                              DocenteRepository docenteRepository) {
        this.especialidadRepository = especialidadRepository;
        this.docenteRepository = docenteRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedIfMissing("Funcional",         "Recuerda traer tus lentes",                                 false, true);
        seedIfMissing("Ortoptica",         "Recuerda traer tus gafas",                                  false, false);
        seedIfMissing("Ciclopejia",        "Trae acompañante y usa gafas de sol despues de la cita",    false, false);
        seedIfMissing("Adaptacion lentes de contacto", null,                                            false, false);
        seedIfMissing("Control",           "Indica el motivo del control al recepcionarte",             true,  false);
        seedIfMissing("Urgencia",          null,                                                        false, false);
        seedIfMissing("Baja vision",       null,                                                        false, false);
        seedIfMissing("Fondo de ojo",      null,                                                        false, false);

        // Asignar la especialidad FUNCIONAL (default) a todos los docentes que aún no la tengan
        Especialidad funcional = especialidadRepository.findByNombreIgnoreCase("Funcional").orElse(null);
        if (funcional != null) {
            List<Docente> docentes = docenteRepository.findAll();
            int actualizados = 0;
            for (Docente d : docentes) {
                if (d.getEspecialidades() == null || !d.getEspecialidades().contains(funcional)) {
                    d.getEspecialidades().add(funcional);
                    docenteRepository.save(d);
                    actualizados++;
                }
            }
            if (actualizados > 0) {
                log.info("EspecialidadSeeder: asignada 'Funcional' a {} docentes", actualizados);
            }
        }
    }

    private void seedIfMissing(String nombre, String observacion, boolean requiereSubtipo, boolean esDefault) {
        especialidadRepository.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Especialidad e = new Especialidad();
            e.setNombre(nombre);
            e.setObservacionRecordatorio(observacion);
            e.setRequiereSubtipo(requiereSubtipo);
            e.setEsDefault(esDefault);
            e.setActivo(true);
            log.info("EspecialidadSeeder: creando especialidad '{}'", nombre);
            return especialidadRepository.save(e);
        });
    }
}
