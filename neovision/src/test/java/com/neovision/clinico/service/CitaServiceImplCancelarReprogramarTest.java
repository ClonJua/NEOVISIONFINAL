package com.neovision.clinico.service;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Especialidad;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.repository.EspecialidadRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.MetaPacientesSemestreRepository;
import com.neovision.clinico.repository.PacienteRepository;
import com.neovision.clinico.repository.SesionPracticaRepository;
import com.neovision.common.exception.AccesoNoAutorizadoException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.entity.Usuario;
import com.neovision.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cancelar y reprogramar citas desde el paciente y el estudiante.
 *
 * Cubre el bug por el que la cancelacion del paciente borraba de la fila el
 * paciente y la fecha de reserva: el limite de 3 cancelaciones nunca se
 * aplicaba, la cita desaparecia del historial y el cupo no se liberaba.
 */
class CitaServiceImplCancelarReprogramarTest {

    @Mock private CitaRepository citaRepository;
    @Mock private SesionPracticaRepository sesionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private AtencionRepository atencionRepository;
    @Mock private MetaPacientesSemestreRepository metaPacientesSemestreRepository;

    private CitaServiceImpl service;
    private Estudiante estudiante;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CitaServiceImpl(citaRepository, sesionRepository, estudianteRepository,
                pacienteRepository, usuarioRepository, emailService, especialidadRepository,
                atencionRepository, metaPacientesSemestreRepository);

        estudiante = new Estudiante();
        estudiante.setIdEstudiante(7); estudiante.setNombre("Jose"); estudiante.setApellido("Navarro");
        estudiante.setCorreo("jose@uni.edu");

        paciente = new Paciente();
        paciente.setIdPaciente(8); paciente.setNombre("Daniela"); paciente.setApellido("Gomez");
        paciente.setCorreo("daniela@mail.com");

        Usuario uPac = new Usuario(); uPac.setIdUsuario(100);
        Usuario uEst = new Usuario(); uEst.setIdUsuario(200);
        when(usuarioRepository.findByCorreo("daniela@mail.com")).thenReturn(Optional.of(uPac));
        when(usuarioRepository.findByCorreo("jose@uni.edu")).thenReturn(Optional.of(uEst));
        when(pacienteRepository.findByUsuarioIdUsuario(100)).thenReturn(Optional.of(paciente));
        when(estudianteRepository.findByUsuarioIdUsuario(200)).thenReturn(Optional.of(estudiante));
        when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Cita cita(int id, String estado, LocalDate fecha, Estudiante est) {
        Docente docente = new Docente();
        docente.setNombre("Jodan"); docente.setApellido("Aparicio");
        docente.setEspecialidades(Collections.<Especialidad>emptySet());

        SesionPractica sesion = new SesionPractica();
        sesion.setIdSesion(id * 10); sesion.setFecha(fecha); sesion.setDocente(docente);
        sesion.setHoraInicio(LocalTime.of(8, 0)); sesion.setHoraFin(LocalTime.of(10, 0));

        Cita c = new Cita();
        c.setIdCita(id);
        c.setSesion(sesion);
        c.setEstudiante(est);
        c.setEstado(estado);
        if ("RESERVADO".equals(estado)) c.setPaciente(paciente);
        when(citaRepository.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    private LocalDate manana() { return LocalDate.now().plusDays(1); }

    // ===================== PACIENTE: CANCELAR =====================

    @Test
    void pacienteCancela_conservaPacienteYLiberaCupoNuevo() {
        Cita c = cita(1, "RESERVADO", manana(), estudiante);

        service.cancelarPaciente("daniela@mail.com", 1, "Viaje");

        assertThat(c.getEstado()).isEqualTo("CANCELADO");
        assertThat(c.getPaciente()).isSameAs(paciente);
        assertThat(c.getCanceladaPor()).isEqualTo("PACIENTE");
        assertThat(c.getFechaCancelacion()).isNotNull();

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository, times(2)).save(captor.capture());
        Cita libre = captor.getAllValues().get(1);
        assertThat(libre).isNotSameAs(c);
        assertThat(libre.getEstado()).isEqualTo("DISPONIBLE");
        assertThat(libre.getPaciente()).isNull();
        assertThat(libre.getSesion()).isSameAs(c.getSesion());
        assertThat(libre.getEstudiante()).isSameAs(estudiante);
    }

    @Test
    void pacienteCancela_conTresCancelacionesPrevias_seRechaza() {
        Cita c = cita(1, "RESERVADO", manana(), estudiante);
        when(citaRepository.countCancelacionesEnMes(eq(8), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.cancelarPaciente("daniela@mail.com", 1, "Viaje"))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("límite de 3 cancelaciones");
        assertThat(c.getEstado()).isEqualTo("RESERVADO");
        verify(citaRepository, never()).save(any());
    }

    // ===================== PACIENTE: REPROGRAMAR =====================

    @Test
    void pacienteReprograma_mueveReservaYLiberaLaAnterior() {
        Especialidad funcional = new Especialidad();
        funcional.setIdEspecialidad(1); funcional.setNombre("Funcional"); funcional.setEsDefault(true);
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        actual.setEspecialidad(funcional);
        Cita nueva = cita(2, "DISPONIBLE", manana().plusDays(1), estudiante);

        service.reprogramarPaciente("daniela@mail.com", 1, 2);

        assertThat(nueva.getEstado()).isEqualTo("RESERVADO");
        assertThat(nueva.getPaciente()).isSameAs(paciente);
        assertThat(nueva.getEspecialidad()).isSameAs(funcional);
        assertThat(actual.getEstado()).isEqualTo("DISPONIBLE");
        assertThat(actual.getPaciente()).isNull();
        assertThat(actual.getEspecialidad()).isNull();
        verify(emailService).enviarReprogramacion(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void pacienteReprograma_citaAjena_seRechaza() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        Paciente otro = new Paciente(); otro.setIdPaciente(99);
        actual.setPaciente(otro);
        cita(2, "DISPONIBLE", manana(), estudiante);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void pacienteReprograma_aBloqueOcupado_seRechaza() {
        cita(1, "RESERVADO", manana(), estudiante);
        cita(2, "RESERVADO", manana(), estudiante);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    void pacienteReprograma_aDocenteSinLaEspecialidad_seRechaza() {
        Especialidad lentes = new Especialidad();
        lentes.setIdEspecialidad(5); lentes.setNombre("Lentes"); lentes.setEsDefault(false);
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        actual.setEspecialidad(lentes);
        cita(2, "DISPONIBLE", manana(), estudiante); // su docente no tiene especialidades

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("no atiende la especialidad");
        assertThat(actual.getEstado()).isEqualTo("RESERVADO");
    }

    @Test
    void pacienteReprograma_aBloquePasado_seRechaza() {
        cita(1, "RESERVADO", manana(), estudiante);
        cita(2, "DISPONIBLE", LocalDate.now().minusDays(1), estudiante);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("ya pasó");
    }

    @Test
    void pacienteReprograma_incrementaContadorQueViajaConLaReserva() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        actual.setReprogramacionesPaciente(1);
        Cita nueva = cita(2, "DISPONIBLE", manana().plusDays(1), estudiante);

        service.reprogramarPaciente("daniela@mail.com", 1, 2);

        assertThat(nueva.getReprogramacionesPaciente()).isEqualTo(2);
        assertThat(actual.getReprogramacionesPaciente()).isZero();
    }

    @Test
    void pacienteReprograma_alcanzadoElLimite_seRechaza() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        actual.setReprogramacionesPaciente(CitaServiceImpl.MAX_REPROGRAMACIONES_PACIENTE);
        cita(2, "DISPONIBLE", manana(), estudiante);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("secretaría");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void estudianteReprograma_noConsumeElLimiteDelPaciente() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        actual.setReprogramacionesPaciente(CitaServiceImpl.MAX_REPROGRAMACIONES_PACIENTE);
        Cita nueva = cita(2, "DISPONIBLE", manana(), estudiante);

        service.reprogramarEstudiante("jose@uni.edu", 1, 2);

        assertThat(nueva.getReprogramacionesPaciente()).isEqualTo(CitaServiceImpl.MAX_REPROGRAMACIONES_PACIENTE);
    }

    @Test
    void reprogramar_conAtencionAbierta_seRechaza() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        cita(2, "DISPONIBLE", manana(), estudiante);
        when(atencionRepository.existsByCitaIdCita(1)).thenReturn(true);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("atención de historia clínica");
        assertThat(actual.getPaciente()).isSameAs(paciente);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reprogramar_aSesionInactiva_seRechaza() {
        cita(1, "RESERVADO", manana(), estudiante);
        Cita nueva = cita(2, "DISPONIBLE", manana(), estudiante);
        nueva.getSesion().setActivo(false);

        assertThatThrownBy(() -> service.reprogramarPaciente("daniela@mail.com", 1, 2))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("no está activa");
    }

    @Test
    void pacienteCancela_motivoDemasiadoLargo_seRechaza() {
        cita(1, "RESERVADO", manana(), estudiante);

        assertThatThrownBy(() -> service.cancelarPaciente("daniela@mail.com", 1, "x".repeat(256)))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("255");
        verify(citaRepository, never()).save(any());
    }

    // ===================== ESTUDIANTE =====================

    @Test
    void estudianteCancela_registraAutorYNoPenalizaAlPaciente() {
        Cita c = cita(1, "RESERVADO", manana(), estudiante);

        service.cancelarEstudiante("jose@uni.edu", 1, "Enfermo");

        assertThat(c.getEstado()).isEqualTo("CANCELADO");
        assertThat(c.getCanceladaPor()).isEqualTo("ESTUDIANTE");
        assertThat(c.getPaciente()).isSameAs(paciente);
        verify(citaRepository, never()).countCancelacionesEnMes(anyInt(), any());
        verify(emailService).enviarCancelacion(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), eq("Estudiante"), eq("Enfermo"));
    }

    @Test
    void estudianteCancela_sinMotivo_seRechaza() {
        cita(1, "RESERVADO", manana(), estudiante);

        assertThatThrownBy(() -> service.cancelarEstudiante("jose@uni.edu", 1, "  "))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void estudianteCancela_bloqueAjeno_seRechaza() {
        Estudiante otro = new Estudiante(); otro.setIdEstudiante(55);
        cita(1, "RESERVADO", manana(), otro);

        assertThatThrownBy(() -> service.cancelarEstudiante("jose@uni.edu", 1, "Enfermo"))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    void estudianteReprograma_entreSusBloques() {
        Cita actual = cita(1, "RESERVADO", manana(), estudiante);
        Cita nueva = cita(2, "DISPONIBLE", manana().plusDays(2), estudiante);

        service.reprogramarEstudiante("jose@uni.edu", 1, 2);

        assertThat(nueva.getPaciente()).isSameAs(paciente);
        assertThat(actual.getEstado()).isEqualTo("DISPONIBLE");
    }

    @Test
    void estudianteReprograma_aBloqueDeOtroEstudiante_seRechaza() {
        Estudiante otro = new Estudiante(); otro.setIdEstudiante(55);
        cita(1, "RESERVADO", manana(), estudiante);
        cita(2, "DISPONIBLE", manana(), otro);

        assertThatThrownBy(() -> service.reprogramarEstudiante("jose@uni.edu", 1, 2))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelarConMenosDeUnaHora_seRechaza() {
        Cita c = cita(1, "RESERVADO", LocalDate.now(), estudiante);
        c.getSesion().setHoraInicio(LocalTime.now().plusMinutes(30));
        c.getSesion().setHoraFin(LocalTime.now().plusMinutes(31));

        assertThatThrownBy(() -> service.cancelarEstudiante("jose@uni.edu", 1, "Enfermo"))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("1 hora de antelación");
        assertThat(List.of(c.getEstado())).containsExactly("RESERVADO");
    }
}
