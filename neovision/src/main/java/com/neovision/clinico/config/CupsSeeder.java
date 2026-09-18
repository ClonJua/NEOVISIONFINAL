package com.neovision.clinico.config;

import com.neovision.clinico.historia.entity.Cups;
import com.neovision.clinico.historia.repository.CupsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga un catálogo CUPS (Clasificación Única de Procedimientos en Salud) de
 * arranque, filtrado a los procedimientos de optometría.
 *

 *
 * <p>Idempotente: solo inserta los códigos que falten.
 */
@Component
@Order(11)
public class CupsSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CupsSeeder.class);

    private final CupsRepository repository;

    public CupsSeeder(CupsRepository repository) {
        this.repository = repository;
    }

    private record Item(String codigo, String descripcion, String categoria) {}

    private static final List<Item> CATALOGO = List.of(
            // Consulta
            new Item("890207", "Consulta de primera vez por optometría", "Consulta"),
            new Item("890307", "Consulta de control o de seguimiento por optometría", "Consulta"),
            // Agudeza visual y campo visual
            new Item("950601", "Medición de agudeza visual", "Agudeza visual y campo visual"),
            new Item("950501", "Estudio de campo visual central y periférico convencional", "Agudeza visual y campo visual"),
            new Item("950505", "Estudio de campo visual central o periférico computarizado", "Agudeza visual y campo visual"),
            // Baja visión
            new Item("950200", "Evaluación de baja visión SOD", "Baja visión"),
            new Item("950201", "Evaluación para baja visión", "Baja visión"),
            // Presión intraocular
            new Item("891100", "Tonometría SOD", "Presión intraocular"),
            new Item("952602", "Curva de presión intraocular (diurna o 24 horas)", "Presión intraocular"),
            // Córnea y segmento anterior
            new Item("951500", "Topografía corneal computarizada SOD", "Córnea y segmento anterior"),
            new Item("951501", "Topografía computada corneal simple", "Córnea y segmento anterior"),
            new Item("951502", "Topografía computada corneal por elevación", "Córnea y segmento anterior"),
            new Item("951503", "Aberrometría ocular", "Córnea y segmento anterior"),
            // Imágenes oculares
            new Item("951101", "Fotografía a color de segmento anterior o posterior del ojo", "Imágenes oculares"),
            new Item("951102", "Fotografía a color de segmento anterior del ojo", "Imágenes oculares"),
            new Item("951103", "Fotografía a color de segmento posterior del ojo", "Imágenes oculares"),
            new Item("951601", "Tomografía óptica coherente de estructura ocular", "Imágenes oculares"),
            // Biometría
            new Item("952000", "Biometría ocular SOD", "Biometría"),
            new Item("952001", "Biometría ocular", "Biometría"),
            // Procedimientos menores
            new Item("965100", "Irrigación y lavado del ojo SOD", "Procedimientos menores"),
            new Item("982101", "Extracción de cuerpo extraño superficial de la conjuntiva", "Procedimientos menores"),
            new Item("982102", "Extracción de cuerpo extraño superficial en córnea o esclerótica", "Procedimientos menores")
    );

    @Override
    public void run(String... args) {
        List<Cups> nuevos = new ArrayList<>();
        for (Item it : CATALOGO) {
            if (!repository.existsById(it.codigo())) {
                nuevos.add(new Cups(it.codigo(), it.descripcion(), it.categoria(), true));
            }
        }
        if (!nuevos.isEmpty()) {
            repository.saveAll(nuevos);
            log.info("CupsSeeder: insertados {} códigos CUPS (fuente: tabla oficial CUPS 2026).", nuevos.size());
        }
    }
}
