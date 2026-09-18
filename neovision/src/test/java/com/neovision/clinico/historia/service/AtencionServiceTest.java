package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.historia.dto.AdendaRequest;
import com.neovision.clinico.historia.dto.DiagnosticoDto;
import com.neovision.clinico.historia.dto.EnviarRevisionRequest;
import com.neovision.clinico.historia.dto.FinalizarAtencionRequest;
import com.neovision.clinico.historia.dto.GuardarAtencionRequest;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.entity.Diagnostico;
import com.neovision.clinico.historia.entity.HistoriaClinica;
import com.neovision.clinico.historia.enums.EstadoAtencion;
import com.neovision.clinico.historia.repository.AdendaRepository;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.historia.repository.Cie10Repository;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.common.exception.AccesoNoAutorizadoHistoriaException;
import com.neovision.common.exception.ContenidoIncompletoException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.HistoriaInmutableException;
import com.neovision.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Cobertura del ciclo de vida de una atención: la ventana de gracia de 2 días
 * (principio de oportunidad, Res. 1995/1999), las transiciones de estado
 * (BORRADOR → PENDIENTE_REVISION → FINALIZADA / DEVUELTA), el contenido mínimo
 * exigido para finalizar, y el sello de integridad (hash) que detecta
 * manipulación de una atención ya finalizada.
 */
class AtencionServiceTest {

    private static final String CORREO_ESTUDIANTE = "estudiante@unbosque.edu.co";
    private static final String CORREO_DOCENTE = "docente@unbosque.edu.co";

    @Mock private AtencionRepository atencionRepository;
    @Mock private HistoriaClinicaRepository historiaRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private AdendaRepository adendaRepository;
    @Mock private Cie10Repository cie10Repository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private HistoriaAccessGuard accessGuard;

    private AtencionService service;
    private Estudiante estudiante;
    private Docente docente;
    private Atencion atencion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AtencionService(atencionRepository, historiaRepository, citaRepository,
                estudianteRepository, docenteRepository, adendaRepository, cie10Repository,
                auditoriaService, accessGuard, new HistoriaMapper());

        estudiante = new Estudiante();
        estudiante.setIdEstudiante(1);
        estudiante.setCorreo(CORREO_ESTUDIANTE);
        Usuario usuarioEst = new Usuario();
        usuarioEst.setFirma("firma-estudiante-perfil");
        estudiante.setUsuario(usuarioEst);

        docente = new Docente();
        docente.setIdDocente(2);
        docente.setCorreo(CORREO_DOCENTE);
        Usuario usuarioDoc = new Usuario();
        usuarioDoc.setFirma("firma-docente-perfil");
        docente.setUsuario(usuarioDoc);

        atencion = new Atencion();
        atencion.setIdAtencion(10);
        atencion.setEstado(EstadoAtencion.BORRADOR);
        atencion.setEstudiante(estudiante);
        atencion.setDocenteSupervisor(docente);
        atencion.setHistoriaClinica(historiaClinicaDe(paciente()));

        when(atencionRepository.findById(10)).thenReturn(Optional.of(atencion));
        when(atencionRepository.save(any(Atencion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(anyInt())).thenReturn(List.of());
        when(cie10Repository.existsByCodigoAndActivoTrue(anyString())).thenReturn(true);
    }

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setIdPaciente(100);
        return p;
    }

    private HistoriaClinica historiaClinicaDe(Paciente p) {
        HistoriaClinica h = new HistoriaClinica();
        h.setIdHistoria(50);
        h.setPaciente(p);
        return h;
    }

    private void asignarCitaConFecha(LocalDate fechaSesion) {
        SesionPractica sesion = new SesionPractica();
        sesion.setFecha(fechaSesion);
        Cita cita = new Cita();
        cita.setSesion(sesion);
        atencion.setCita(cita);
    }

    private Authentication authComo(String correo) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(correo);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(
                correo.equals(CORREO_DOCENTE) ? "ROLE_DOCENTE" : "ROLE_ESTUDIANTE"));
        org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }

    private GuardarAtencionRequest requestVacio() {
        return new GuardarAtencionRequest();
    }

    // ===================== Ventana de gracia (guardarBorrador) =====================

    @Test
    void guardarBorrador_dentroDeLaVentanaDeGracia_permiteEditar() {
        asignarCitaConFecha(LocalDate.now().minusDays(1));
        assertThatCode(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarBorrador_exactamenteEnElLimiteDeLaVentana_permiteEditar() {
        // fecha + 2 dias == hoy: "isAfter" debe ser false, así que todavía es editable.
        asignarCitaConFecha(LocalDate.now().minusDays(2));
        assertThatCode(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarBorrador_unDiaDespuesDelLimite_lanzaHistoriaInmutable() {
        asignarCitaConFecha(LocalDate.now().minusDays(3));
        assertThatThrownBy(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(HistoriaInmutableException.class)
                .hasMessageContaining("ventana");
    }

    // La ventana se anclaba solo a la fecha de la cita, y ese reloj seguía corriendo
    // mientras la atención esperaba revisión. Si el docente tardaba, el estudiante
    // recibía una devolución que ya no podía corregir: solo reenviarla igual, con lo
    // que la corrección pedida nunca podía hacerse.

    @Test
    void guardarBorrador_devolucionTardia_reabreLaVentana() {
        asignarCitaConFecha(LocalDate.now().minusDays(10)); // ventana de la cita: cerrada hace mucho
        atencion.setEstado(EstadoAtencion.DEVUELTA);
        atencion.setFechaDevolucion(java.time.LocalDateTime.now());

        assertThatCode(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarBorrador_ventanaDeLaDevolucion_tambienExpira() {
        // El plazo se reabre, pero sigue acotado: no se edita indefinidamente.
        asignarCitaConFecha(LocalDate.now().minusDays(10));
        atencion.setEstado(EstadoAtencion.DEVUELTA);
        atencion.setFechaDevolucion(java.time.LocalDateTime.now().minusDays(3));

        assertThatThrownBy(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(HistoriaInmutableException.class)
                .hasMessageContaining("ventana");
    }

    @Test
    void guardarBorrador_devolucionVieja_noRecortaLaVentanaDeLaCita() {
        // Se toma el mayor de los dos plazos: una devolución antigua no puede dejar
        // al estudiante peor de lo que estaba por la fecha de la cita.
        asignarCitaConFecha(LocalDate.now());
        atencion.setEstado(EstadoAtencion.DEVUELTA);
        atencion.setFechaDevolucion(java.time.LocalDateTime.now().minusDays(9));

        assertThatCode(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarBorrador_sinCitaAsociada_noRestringePorFecha() {
        // Sin cita, fechaCita() es null: la validación de ventana se omite (no hay fecha contra la cual medir).
        assertThatCode(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarBorrador_finalizada_lanzaHistoriaInmutableSinImportarLaFecha() {
        asignarCitaConFecha(LocalDate.now()); // fecha valida, no deberia importar
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        assertThatThrownBy(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(HistoriaInmutableException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    void guardarBorrador_pendienteRevision_lanzaAccesoNoAutorizado() {
        asignarCitaConFecha(LocalDate.now());
        atencion.setEstado(EstadoAtencion.PENDIENTE_REVISION);
        assertThatThrownBy(() -> service.guardarBorrador(10, requestVacio(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(AccesoNoAutorizadoHistoriaException.class);
    }

    @Test
    void guardarBorrador_noEsElEstudianteDuenio_lanzaAccesoNoAutorizado() {
        asignarCitaConFecha(LocalDate.now());
        assertThatThrownBy(() -> service.guardarBorrador(10, requestVacio(), authComo("otro.estudiante@unbosque.edu.co")))
                .isInstanceOf(AccesoNoAutorizadoHistoriaException.class);
    }

    // ===================== enviarRevision NO aplica la ventana de gracia =====================

    @Test
    void enviarRevision_fueraDeLaVentanaDeGracia_igualPermiteEnviar() {
        // A proposito: enviarRevision() no llama requireEditable(), asi que una atención
        // cuya ventana de edición ya cerró todavía debe poder enviarse a revisión (si no,
        // quedaría huérfana en BORRADOR sin ninguna salida posible).
        asignarCitaConFecha(LocalDate.now().minusDays(30));
        var resp = service.enviarRevision(10, new EnviarRevisionRequest(), authComo(CORREO_ESTUDIANTE));
        assertThat(resp.getEstado()).isEqualTo(EstadoAtencion.PENDIENTE_REVISION.name());
        assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.PENDIENTE_REVISION);
    }

    @Test
    void enviarRevision_usaLaFirmaDelPerfilDelEstudianteNoLaDelRequest() {
        asignarCitaConFecha(LocalDate.now());
        EnviarRevisionRequest req = new EnviarRevisionRequest();
        req.setFirmaEstudiante("firma-enviada-por-el-cliente");

        service.enviarRevision(10, req, authComo(CORREO_ESTUDIANTE));

        assertThat(atencion.getFirmaEstudiante()).isEqualTo("firma-estudiante-perfil");
    }

    @Test
    void enviarRevision_sinFirmaEnElPerfil_usaLaDelRequestComoRespaldo() {
        asignarCitaConFecha(LocalDate.now());
        estudiante.getUsuario().setFirma(null);
        EnviarRevisionRequest req = new EnviarRevisionRequest();
        req.setFirmaEstudiante("firma-de-respaldo");

        service.enviarRevision(10, req, authComo(CORREO_ESTUDIANTE));

        assertThat(atencion.getFirmaEstudiante()).isEqualTo("firma-de-respaldo");
    }

    @Test
    void enviarRevision_finalizada_lanzaHistoriaInmutable() {
        asignarCitaConFecha(LocalDate.now());
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        assertThatThrownBy(() -> service.enviarRevision(10, new EnviarRevisionRequest(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(HistoriaInmutableException.class);
    }

    @Test
    void enviarRevision_yaEstaEnRevision_lanzaEstadoInvalido() {
        asignarCitaConFecha(LocalDate.now());
        atencion.setEstado(EstadoAtencion.PENDIENTE_REVISION);
        assertThatThrownBy(() -> service.enviarRevision(10, new EnviarRevisionRequest(), authComo(CORREO_ESTUDIANTE)))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    // ===================== finalizar: contenido mínimo y co-firma obligatoria =====================

    private void prepararParaFinalizar() {
        asignarCitaConFecha(LocalDate.now());
        atencion.setEstado(EstadoAtencion.PENDIENTE_REVISION);
        atencion.setMotivoConsulta("Control visual de rutina");
        Diagnostico d = new Diagnostico();
        d.setCodigoCie10("H52.1");
        d.setDescripcion("Miopía");
        d.setOrden(1);
        atencion.getDiagnosticos().add(d);
    }

    @Test
    void finalizar_sinMotivoConsulta_lanzaContenidoIncompleto() {
        prepararParaFinalizar();
        atencion.setMotivoConsulta(null);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(ContenidoIncompletoException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void finalizar_sinDiagnosticos_lanzaContenidoIncompleto() {
        prepararParaFinalizar();
        atencion.getDiagnosticos().clear();
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(ContenidoIncompletoException.class)
                .hasMessageContaining("diagnóstico");
    }

    @Test
    void finalizar_codigoCie10NoEstaEnElCatalogo_lanzaContenidoIncompleto() {
        prepararParaFinalizar();
        when(cie10Repository.existsByCodigoAndActivoTrue("H52.1")).thenReturn(false);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(ContenidoIncompletoException.class)
                .hasMessageContaining("CIE-10");
    }

    @Test
    void finalizar_docenteSinFirmaRegistradaYSinRespaldoEnRequest_lanzaContenidoIncompleto() {
        prepararParaFinalizar();
        docente.getUsuario().setFirma(null);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(ContenidoIncompletoException.class)
                .hasMessageContaining("firma");
    }

    @Test
    void finalizar_noEstaEnRevision_lanzaEstadoInvalido() {
        prepararParaFinalizar();
        atencion.setEstado(EstadoAtencion.BORRADOR);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void finalizar_devuelta_explicaAlDocenteCualEsLaSalida() {
        // Antes decia solo "Solo se pueden finalizar atenciones que estan en revision",
        // y el docente no sabia que la salida es pedirle al estudiante que la reenvie.
        prepararParaFinalizar();
        atencion.setEstado(EstadoAtencion.DEVUELTA);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("reenvíe")
                .hasMessageContaining("adenda");
    }

    @Test
    void finalizar_yaFinalizada_lanzaHistoriaInmutable() {
        prepararParaFinalizar();
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        assertThatThrownBy(() -> service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE)))
                .isInstanceOf(HistoriaInmutableException.class);
    }

    @Test
    void finalizar_flujoCompleto_quedaFinalizadaConSelloDeIntegridad() {
        prepararParaFinalizar();
        var resp = service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE));

        assertThat(resp.getEstado()).isEqualTo(EstadoAtencion.FINALIZADA.name());
        assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.FINALIZADA);
        assertThat(atencion.getFirmaDocente()).isEqualTo("firma-docente-perfil");
        assertThat(atencion.getHashIntegridad()).isNotBlank();
    }

    // ===================== Sello de integridad: detección de manipulación =====================

    @Test
    void verificarIntegridad_atencionNoFinalizada_indicaQueNoTieneSello() {
        prepararParaFinalizar();
        atencion.setEstado(EstadoAtencion.PENDIENTE_REVISION); // aun no finalizada

        var resp = service.verificarIntegridad(10, authComo(CORREO_DOCENTE));

        assertThat(resp.tieneSello()).isFalse();
        assertThat(resp.valido()).isFalse();
    }

    @Test
    void verificarIntegridad_contenidoIntacto_esValido() {
        prepararParaFinalizar();
        service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE));

        var resp = service.verificarIntegridad(10, authComo(CORREO_DOCENTE));

        assertThat(resp.tieneSello()).isTrue();
        assertThat(resp.valido()).isTrue();
        assertThat(resp.hashRecalculado()).isEqualTo(resp.hashAlmacenado());
    }

    @Test
    void verificarIntegridad_contenidoAlteradoTrasFinalizar_detectaManipulacion() {
        prepararParaFinalizar();
        service.finalizar(10, new FinalizarAtencionRequest(), authComo(CORREO_DOCENTE));

        // Simula una alteracion directa en BD del contenido clinico ya sellado.
        atencion.setMotivoConsulta("Motivo alterado despues de finalizar");

        var resp = service.verificarIntegridad(10, authComo(CORREO_DOCENTE));

        assertThat(resp.tieneSello()).isTrue();
        assertThat(resp.valido()).isFalse();
        assertThat(resp.hashRecalculado()).isNotEqualTo(resp.hashAlmacenado());
    }

    // ===================== Adendas: solo quien participo puede agregar =====================
    // Antes agregarAdenda() no verificaba nada mas que el estado FINALIZADA:
    // cualquier estudiante o docente autenticado podia anexar una adenda a la
    // atencion de un paciente que nunca atendio. Como las adendas son de solo
    // anexar (append-only), eso habria quedado ahi para siempre.

    private AdendaRequest requestAdenda() {
        AdendaRequest r = new AdendaRequest();
        r.setTexto("Se corrige la agudeza visual del ojo derecho registrada en el folio 2");
        return r;
    }

    @Test
    void agregarAdenda_elEstudianteDeLaAtencion_puede() {
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        var resp = service.agregarAdenda(10, requestAdenda(), authComo(CORREO_ESTUDIANTE));
        assertThat(resp).isNotNull();
    }

    @Test
    void agregarAdenda_elDocenteSupervisor_puede() {
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        var resp = service.agregarAdenda(10, requestAdenda(), authComo(CORREO_DOCENTE));
        assertThat(resp).isNotNull();
    }

    @Test
    void agregarAdenda_elEstudianteQueEsLaParejaDeLaCita_puede() {
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        Estudiante pareja = new Estudiante();
        pareja.setIdEstudiante(3);
        pareja.setCorreo("pareja@unbosque.edu.co");
        Cita cita = new Cita();
        cita.setEstudiante(estudiante);
        cita.setEstudiante2(pareja);
        atencion.setCita(cita);

        var resp = service.agregarAdenda(10, requestAdenda(), authComo("pareja@unbosque.edu.co"));
        assertThat(resp).isNotNull();
    }

    @Test
    void agregarAdenda_unEstudianteAjenoALaAtencion_seRechaza() {
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        assertThatThrownBy(() -> service.agregarAdenda(10, requestAdenda(), authComo("intruso@unbosque.edu.co")))
                .isInstanceOf(AccesoNoAutorizadoHistoriaException.class)
                .hasMessageContaining("no es tuya");
    }

    @Test
    void agregarAdenda_unDocenteQueNoEsElSupervisor_seRechaza() {
        atencion.setEstado(EstadoAtencion.FINALIZADA);
        assertThatThrownBy(() -> service.agregarAdenda(10, requestAdenda(), authComo("otro.docente@unbosque.edu.co")))
                .isInstanceOf(AccesoNoAutorizadoHistoriaException.class);
    }

    @Test
    void agregarAdenda_siNoEstaFinalizada_seRechazaAunSiendoElDueno() {
        atencion.setEstado(EstadoAtencion.BORRADOR);
        assertThatThrownBy(() -> service.agregarAdenda(10, requestAdenda(), authComo(CORREO_ESTUDIANTE)))
                .hasMessageContaining("finalizadas");
    }
}
