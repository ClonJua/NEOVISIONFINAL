package com.neovision.clinico.config;

import com.neovision.clinico.historia.entity.Cie10;
import com.neovision.clinico.historia.repository.Cie10Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga un catálogo CIE-10 curado de diagnósticos frecuentes en optometría y
 * oftalmología, para que los diagnósticos de las atenciones se registren contra
 * un vocabulario controlado (no texto libre). Idempotente: solo inserta los
 * códigos que falten.
 */
@Component
@Order(10)
public class Cie10Seeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Cie10Seeder.class);

    private final Cie10Repository repository;

    public Cie10Seeder(Cie10Repository repository) {
        this.repository = repository;
    }

    private record Item(String codigo, String descripcion, String categoria) {}

    private static final List<Item> CATALOGO = List.of(
            // Trastornos de la refracción y de la acomodación
            new Item("H52.0", "Hipermetropía", "Refracción y acomodación"),
            new Item("H52.1", "Miopía", "Refracción y acomodación"),
            new Item("H52.2", "Astigmatismo", "Refracción y acomodación"),
            new Item("H52.3", "Anisometropía y aniseiconía", "Refracción y acomodación"),
            new Item("H52.4", "Presbicia", "Refracción y acomodación"),
            new Item("H52.5", "Trastornos de la acomodación", "Refracción y acomodación"),
            new Item("H52.6", "Otros trastornos de la refracción", "Refracción y acomodación"),
            new Item("H52.7", "Trastorno de la refracción, no especificado", "Refracción y acomodación"),
            // Estrabismo y visión binocular
            new Item("H50.0", "Estrabismo concomitante convergente (esotropía)", "Estrabismo y binocularidad"),
            new Item("H50.1", "Estrabismo concomitante divergente (exotropía)", "Estrabismo y binocularidad"),
            new Item("H50.4", "Otros estrabismos y los no especificados (heteroforias)", "Estrabismo y binocularidad"),
            new Item("H53.0", "Ambliopía ex anopsia", "Estrabismo y binocularidad"),
            new Item("H53.2", "Diplopía", "Estrabismo y binocularidad"),
            // Alteraciones visuales
            new Item("H53.1", "Alteraciones visuales subjetivas", "Alteraciones visuales"),
            new Item("H53.4", "Defectos del campo visual", "Alteraciones visuales"),
            new Item("H53.5", "Deficiencias de la visión cromática", "Alteraciones visuales"),
            new Item("H53.9", "Alteración visual, no especificada", "Alteraciones visuales"),
            new Item("H54.9", "Discapacidad visual, no especificada", "Alteraciones visuales"),
            // Conjuntiva, córnea y superficie ocular
            new Item("H10.1", "Conjuntivitis atópica aguda", "Superficie ocular"),
            new Item("H10.4", "Conjuntivitis crónica", "Superficie ocular"),
            new Item("H10.9", "Conjuntivitis, no especificada", "Superficie ocular"),
            new Item("H11.0", "Pterigión", "Superficie ocular"),
            new Item("H04.1", "Síndrome de ojo seco (otros trastornos de la glándula lagrimal)", "Superficie ocular"),
            new Item("H16.0", "Úlcera de la córnea", "Superficie ocular"),
            new Item("H16.9", "Queratitis, no especificada", "Superficie ocular"),
            // Cristalino, glaucoma y segmento posterior
            new Item("H25.9", "Catarata senil, no especificada", "Cristalino y segmento posterior"),
            new Item("H40.9", "Glaucoma, no especificado", "Cristalino y segmento posterior"),
            new Item("H35.0", "Retinopatía del fondo y cambios vasculares retinianos", "Cristalino y segmento posterior"),
            new Item("H35.3", "Degeneración de la mácula y del polo posterior", "Cristalino y segmento posterior"),
            new Item("H43.9", "Trastorno del cuerpo vítreo, no especificado", "Cristalino y segmento posterior"),
            // Síntomas y exámenes
            new Item("H57.1", "Dolor ocular", "Síntomas y exámenes"),
            new Item("H57.9", "Trastorno del ojo y sus anexos, no especificado", "Síntomas y exámenes"),
            new Item("Z01.0", "Examen de ojos y de la visión (control)", "Síntomas y exámenes")
    );

    @Override
    public void run(String... args) {
        List<Cie10> nuevos = new ArrayList<>();
        for (Item it : CATALOGO) {
            if (!repository.existsById(it.codigo())) {
                nuevos.add(new Cie10(it.codigo(), it.descripcion(), it.categoria(), true));
            }
        }
        if (!nuevos.isEmpty()) {
            repository.saveAll(nuevos);
            log.info("Cie10Seeder: insertados {} códigos CIE-10.", nuevos.size());
        }
    }
}
