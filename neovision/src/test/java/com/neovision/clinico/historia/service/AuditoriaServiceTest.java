package com.neovision.clinico.historia.service;

import com.neovision.clinico.historia.dto.VerificacionCadenaResponse;
import com.neovision.clinico.historia.entity.Auditoria;
import com.neovision.clinico.historia.enums.AccionAuditoria;
import com.neovision.clinico.historia.repository.AuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cobertura de la cadena de hashes de la bitácora de auditoría: que registrar()
 * encadene cada evento con el hash del anterior, y que verificarCadena()
 * detecte tanto un registro con contenido alterado (hash no coincide) como un
 * enlace roto entre dos eventos (hashPrevio no coincide) — los dos escenarios
 * de manipulación que la cadena está pensada para exponer.
 */
class AuditoriaServiceTest {

    @Mock private AuditoriaRepository auditoriaRepository;

    private AuditoriaService service;
    private final List<Auditoria> almacen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuditoriaService(auditoriaRepository);
        almacen.clear();

        // Mock con estado propio: simula la tabla de auditoría en memoria para poder
        // encadenar varias llamadas a registrar() y luego releer todo con verificarCadena().
        when(auditoriaRepository.findTopByOrderByIdAuditoriaDesc()).thenAnswer(inv ->
                almacen.isEmpty() ? Optional.empty() : Optional.of(almacen.get(almacen.size() - 1)));
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(inv -> {
            Auditoria a = inv.getArgument(0);
            if (a.getIdAuditoria() == null) a.setIdAuditoria((long) (almacen.size() + 1));
            almacen.add(a);
            return a;
        });
        when(auditoriaRepository.findAllByOrderByIdAuditoriaAsc()).thenAnswer(inv -> new ArrayList<>(almacen));
    }

    private void registrarEvento(String correo, String detalle) {
        service.registrar(correo, "ROLE_ESTUDIANTE", AccionAuditoria.CONSULTAR, "ATENCION", 1, 10, detalle);
    }

    @Test
    void cadenaVacia_esValidaPorDefecto() {
        VerificacionCadenaResponse r = service.verificarCadena();
        assertThat(r.isValida()).isTrue();
        assertThat(r.getTotalRegistros()).isZero();
        assertThat(r.getIdRegistroAlterado()).isNull();
    }

    @Test
    void variosRegistrosSeguidos_formanUnaCadenaValidaMientrasNadieLaAltere() {
        registrarEvento("a@x.com", "uno");
        registrarEvento("b@x.com", "dos");
        registrarEvento("c@x.com", "tres");

        VerificacionCadenaResponse r = service.verificarCadena();

        assertThat(r.isValida()).isTrue();
        assertThat(r.getTotalRegistros()).isEqualTo(3);
        assertThat(r.getIdRegistroAlterado()).isNull();
        // Cada registro debe encadenar con el hash real del anterior.
        assertThat(almacen.get(1).getHashPrevio()).isEqualTo(almacen.get(0).getHashRegistro());
        assertThat(almacen.get(2).getHashPrevio()).isEqualTo(almacen.get(1).getHashRegistro());
    }

    @Test
    void primerRegistro_encadenaConElValorGenesis() {
        registrarEvento("a@x.com", "uno");
        assertThat(almacen.get(0).getHashPrevio()).isEqualTo("GENESIS");
    }

    @Test
    void detectaContenidoAlteradoDeUnRegistro() {
        registrarEvento("a@x.com", "detalle original");
        registrarEvento("b@x.com", "otro evento");

        // Simula una alteración directa en BD: se cambia el contenido sin recalcular el hash.
        Auditoria alterado = almacen.get(0);
        alterado.setDetalle("detalle manipulado");

        VerificacionCadenaResponse r = service.verificarCadena();

        assertThat(r.isValida()).isFalse();
        assertThat(r.getIdRegistroAlterado()).isEqualTo(alterado.getIdAuditoria());
        assertThat(r.getMensaje()).contains("alterado");
    }

    @Test
    void detectaEnlaceRotoEntreDosRegistros() {
        registrarEvento("a@x.com", "uno");
        registrarEvento("b@x.com", "dos");

        // Rompe el enlace: el segundo registro ya no apunta al hash real del primero.
        Auditoria segundo = almacen.get(1);
        segundo.setHashPrevio("hash-falso");

        VerificacionCadenaResponse r = service.verificarCadena();

        assertThat(r.isValida()).isFalse();
        assertThat(r.getIdRegistroAlterado()).isEqualTo(segundo.getIdAuditoria());
        assertThat(r.getMensaje()).contains("no enlaza");
    }
}
