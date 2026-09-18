package com.neovision.clinico.service;

import com.neovision.clinico.dto.SesionRequest;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.SalonRepository;
import com.neovision.clinico.repository.SesionPracticaRepository;
import com.neovision.common.exception.EstadoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cobertura de las reglas de fecha para sesiones de práctica: fechas pasadas,
 * fines de semana, y el calendario de festivos colombianos (fijos, Ley Emiliani
 * y móviles atados a la Pascua). Las fechas de referencia de 2026 usadas aquí
 * (Pascua = 5 de abril, 6 de enero = martes) se verificaron de forma
 * independiente con el algoritmo de Meeus/Jones/Butcher antes de escribir este
 * test, para no limitarse a comprobar la implementación contra sí misma.
 */
class SesionServiceImplTest {

    @Mock private SesionPracticaRepository sesionRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private CitaRepository citaRepository;

    private SesionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SesionServiceImpl(sesionRepository, salonRepository, docenteRepository,
                estudianteRepository, citaRepository);
    }

    /** Invoca el método privado validarFechaSesion(LocalDate): lógica pura, sin dependencias de BD. */
    private void validar(LocalDate fecha) throws Exception {
        Method m = SesionServiceImpl.class.getDeclaredMethod("validarFechaSesion", LocalDate.class);
        m.setAccessible(true);
        try {
            m.invoke(service, fecha);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    /** Invoca el método privado esFestivo(LocalDate) directamente, sin pasar por el chequeo de "fecha pasada". */
    private boolean esFestivo(LocalDate fecha) throws Exception {
        Method m = SesionServiceImpl.class.getDeclaredMethod("esFestivo", LocalDate.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, fecha);
    }

    // ===================== Fecha pasada / fin de semana =====================

    @Test
    void rechazaFechaPasada() {
        assertThatThrownBy(() -> validar(LocalDate.now().minusDays(1)))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("pasadas");
    }

    @Test
    void rechazaSabado() throws Exception {
        LocalDate sabado = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        assertThatThrownBy(() -> validar(sabado))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("sábados");
    }

    @Test
    void rechazaDomingo() throws Exception {
        LocalDate domingo = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        assertThatThrownBy(() -> validar(domingo))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("domingos");
    }

    // ===================== Festivos fijos =====================

    @Test
    void festivosFijos_2026() throws Exception {
        assertThat(esFestivo(LocalDate.of(2026, 1, 1))).as("Año Nuevo").isTrue();
        assertThat(esFestivo(LocalDate.of(2026, 5, 1))).as("Día del Trabajo").isTrue();
        assertThat(esFestivo(LocalDate.of(2026, 7, 20))).as("Grito de Independencia").isTrue();
        assertThat(esFestivo(LocalDate.of(2026, 12, 25))).as("Navidad").isTrue();
    }

    // ===================== Ley Emiliani: traslado al lunes siguiente =====================

    @Test
    void leyEmiliani_reyesMagos2026_noCaeEnLunes_seTrasladaAlLunesSiguiente() throws Exception {
        LocalDate nominal = LocalDate.of(2026, 1, 6);
        assertThat(nominal.getDayOfWeek()).as("verificado de forma independiente: 6-ene-2026 es martes")
                .isEqualTo(DayOfWeek.TUESDAY);
        LocalDate trasladado = nominal.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        assertThat(esFestivo(nominal))
                .as("la fecha nominal (no-lunes) no debe quedar bloqueada: el festivo ya se trasladó")
                .isFalse();
        assertThat(esFestivo(trasladado))
                .as("el lunes siguiente es el festivo realmente observado")
                .isTrue();
    }

    @Test
    void leyEmiliani_reyesMagos2025_yaCaeEnLunes_noSeTraslada() throws Exception {
        LocalDate nominal = LocalDate.of(2025, 1, 6);
        assertThat(nominal.getDayOfWeek()).as("verificado de forma independiente: 6-ene-2025 es lunes")
                .isEqualTo(DayOfWeek.MONDAY);
        assertThat(esFestivo(nominal)).isTrue();
    }

    // ===================== Semana Santa y festivos móviles atados a la Pascua =====================

    @Test
    void semanaSanta2026_juevesYViernesSanto_nuncaSeTrasladan() throws Exception {
        // Pascua 2026 = 5-abr (verificado de forma independiente con Meeus/Jones/Butcher).
        assertThat(esFestivo(LocalDate.of(2026, 4, 2))).as("Jueves Santo").isTrue();
        assertThat(esFestivo(LocalDate.of(2026, 4, 3))).as("Viernes Santo").isTrue();
    }

    @Test
    void festivoMovilAtadoALaPascua_ascension2026_seTrasladaAlLunes() throws Exception {
        LocalDate pascua2026 = LocalDate.of(2026, 4, 5);
        LocalDate ascensionNominal = pascua2026.plusDays(39);
        LocalDate ascensionObservada = ascensionNominal.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        assertThat(ascensionNominal.getDayOfWeek()).isNotEqualTo(DayOfWeek.MONDAY);
        assertThat(esFestivo(ascensionNominal)).isFalse();
        assertThat(esFestivo(ascensionObservada)).isTrue();
    }

    @Test
    void diaOrdinarioSinFestivosCercanos_noEsFestivo() throws Exception {
        // Martes ordinario de 2026, lejos de cualquier festivo fijo o móvil.
        assertThat(esFestivo(LocalDate.of(2026, 9, 15))).isFalse();
    }

    // ===================== crearSesion / editarSesion: integración con la validación =====================

    @Test
    void crearSesion_fechaInvalida_fallaAntesDeConsultarRepositorios() {
        SesionRequest req = new SesionRequest();
        req.setIdDocente(1);
        req.setFecha(LocalDate.now().minusDays(1).toString());
        req.setCuposTotales(3);

        assertThatThrownBy(() -> service.crearSesion(req)).isInstanceOf(EstadoInvalidoException.class);
        verifyNoInteractions(docenteRepository, salonRepository, sesionRepository);
    }

    @Test
    void editarSesion_siNoVieneFechaNueva_noRevalidaLaFechaExistente() {
        // Regresión: antes solo crearSesion() validaba fechas; editar una sesión sin tocar
        // su fecha no debe fallar aunque esa fecha (ya guardada) haya quedado en el pasado.
        SesionPractica existente = new SesionPractica();
        existente.setIdSesion(1);
        existente.setFecha(LocalDate.now().minusDays(10));
        existente.setCuposTotales(3);
        existente.setEstudiantes(new java.util.ArrayList<>());

        when(sesionRepository.findById(1)).thenReturn(Optional.of(existente));
        com.neovision.clinico.entity.Docente docente = new com.neovision.clinico.entity.Docente();
        docente.setIdDocente(9);
        when(docenteRepository.findById(9)).thenReturn(Optional.of(docente));
        when(citaRepository.findBySesionIdSesion(1)).thenReturn(java.util.List.of());

        SesionRequest req = new SesionRequest();
        req.setIdDocente(9);
        req.setCuposTotales(5);
        req.setFecha(null);

        assertThatCode(() -> service.editarSesion(1, req)).doesNotThrowAnyException();
        assertThat(existente.getFecha()).isEqualTo(LocalDate.now().minusDays(10));
    }

    @Test
    void editarSesion_siCambiaLaFecha_laValidaComoUnaCreacionNueva() {
        SesionPractica existente = new SesionPractica();
        existente.setIdSesion(1);
        existente.setFecha(LocalDate.now().plusDays(5));
        existente.setCuposTotales(3);

        when(sesionRepository.findById(1)).thenReturn(Optional.of(existente));
        com.neovision.clinico.entity.Docente docente = new com.neovision.clinico.entity.Docente();
        docente.setIdDocente(9);
        when(docenteRepository.findById(9)).thenReturn(Optional.of(docente));

        SesionRequest req = new SesionRequest();
        req.setIdDocente(9);
        req.setCuposTotales(5);
        req.setFecha(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).toString());

        assertThatThrownBy(() -> service.editarSesion(1, req)).isInstanceOf(EstadoInvalidoException.class);
    }
}
