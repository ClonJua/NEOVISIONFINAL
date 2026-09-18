package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.enums.EstadoSolicitud;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import com.neovision.clinico.historia.repository.SolicitudHistoriaRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el bug reportado: un paciente sin historia clínica abierta (nunca
 * atendido) podía crear una solicitud de copia igual — quedaba PENDIENTE y
 * solo fallaba después, cuando el admin intentaba aprobarla y generar el PDF.
 * Ahora se rechaza al momento de crearla, con un mensaje claro para el paciente.
 */
class SolicitudHistoriaServiceTest {

    @Mock private SolicitudHistoriaRepository solicitudRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HistoriaClinicaRepository historiaClinicaRepository;
    @Mock private HistoriaPdfService pdfService;
    @Mock private EmailService emailService;
    @Mock private Authentication auth;

    private SolicitudHistoriaService service;

    private static final Integer ID_PACIENTE = 8;
    private static final String CORREO = "dagomezl@ufpso.edu.co";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SolicitudHistoriaService(solicitudRepository, pacienteRepository,
                usuarioRepository, historiaClinicaRepository, pdfService, emailService);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(20);
        usuario.setCorreo(CORREO);
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(ID_PACIENTE);
        paciente.setUsuario(usuario);

        when(auth.getName()).thenReturn(CORREO);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(java.util.Optional.of(usuario));
        when(pacienteRepository.findByUsuarioIdUsuario(20)).thenReturn(java.util.Optional.of(paciente));
    }

    @Test
    void sinHistoriaClinica_seRechaza() {
        when(historiaClinicaRepository.existsByPacienteIdPaciente(ID_PACIENTE)).thenReturn(false);

        assertThatThrownBy(() -> service.crear(null, auth))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("no tienes una historia clínica");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void conHistoriaClinicaYSinPendiente_seCrea() {
        when(historiaClinicaRepository.existsByPacienteIdPaciente(ID_PACIENTE)).thenReturn(true);
        when(solicitudRepository.existsByPacienteIdPacienteAndEstado(ID_PACIENTE, EstadoSolicitud.PENDIENTE))
                .thenReturn(false);
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.crear(null, auth)).isNotNull();
        verify(solicitudRepository).save(any());
    }

    @Test
    void conHistoriaClinicaPeroYaHayPendiente_seRechaza() {
        when(historiaClinicaRepository.existsByPacienteIdPaciente(ID_PACIENTE)).thenReturn(true);
        when(solicitudRepository.existsByPacienteIdPacienteAndEstado(ID_PACIENTE, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(null, auth))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("pendiente");

        verify(solicitudRepository, never()).save(any());
    }

    // ===================== CANAL PRESENCIAL =====================
    // El derecho de acceso a la historia (Res. 1995/1999, Ley 1581/2012) no depende
    // de tener correo ni cuenta: un adulto mayor la pide en la clinica y la
    // secretaria la registra en su nombre. Lo que debe quedar trazado es que
    // ejercio el derecho y quien verifico su identidad.

    @Test
    void presencial_registraSolicitudAprobadaYDevuelvePdf() {
        Paciente sinCuenta = new Paciente();
        sinCuenta.setIdPaciente(99);
        sinCuenta.setNombre("Rosa");
        sinCuenta.setApellido("Medina");

        when(pacienteRepository.findById(99)).thenReturn(java.util.Optional.of(sinCuenta));
        when(historiaClinicaRepository.existsByPacienteIdPaciente(99)).thenReturn(true);
        when(pdfService.generarPorPaciente(99, auth)).thenReturn(new byte[]{1, 2, 3});
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] pdf = service.crearPresencial(99, "Consulta externa", auth);

        assertThat(pdf).containsExactly(1, 2, 3);

        var guardada = org.mockito.ArgumentCaptor.forClass(
                com.neovision.clinico.historia.entity.SolicitudHistoria.class);
        verify(solicitudRepository).save(guardada.capture());

        // Nace resuelta: el paciente esta presente y se le entrega la copia en el acto.
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(guardada.getValue().getCanal())
                .isEqualTo(com.neovision.clinico.historia.enums.CanalSolicitud.PRESENCIAL);
        // Aqui la identidad no la verifica la autenticacion, sino quien la registra.
        assertThat(guardada.getValue().getCreadaPorCorreo()).isEqualTo(CORREO);
        assertThat(guardada.getValue().getFechaResolucion()).isNotNull();

        // No se envia correo: la entrega es en mano.
        verify(emailService, never()).enviarHistoriaClinica(any(), any(), any(), any());
    }

    @Test
    void presencial_sinHistoriaClinica_seRechaza() {
        Paciente sinCuenta = new Paciente();
        sinCuenta.setIdPaciente(99);
        when(pacienteRepository.findById(99)).thenReturn(java.util.Optional.of(sinCuenta));
        when(historiaClinicaRepository.existsByPacienteIdPaciente(99)).thenReturn(false);

        assertThatThrownBy(() -> service.crearPresencial(99, null, auth))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("historia clínica");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void aprobar_pacienteSinCorreo_resuelveSinEnviarCorreo() {
        Paciente sinCorreo = new Paciente();
        sinCorreo.setIdPaciente(99);
        sinCorreo.setNombre("Rosa");
        sinCorreo.setApellido("Medina");
        sinCorreo.setCorreo(null);

        var solicitud = new com.neovision.clinico.historia.entity.SolicitudHistoria();
        solicitud.setIdSolicitud(3);
        solicitud.setPaciente(sinCorreo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        when(solicitudRepository.findById(3)).thenReturn(java.util.Optional.of(solicitud));
        when(pdfService.generarPorPaciente(99, auth)).thenReturn(new byte[]{9});
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.aprobar(3, auth);

        // Queda resuelta igual, pero sin intentar un envio imposible.
        assertThat(response.getEstado()).isEqualTo(EstadoSolicitud.APROBADA.name());
        verify(emailService, never()).enviarHistoriaClinica(any(), any(), any(), any());
    }
}
