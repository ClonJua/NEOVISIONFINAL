package com.neovision.clinico.service;

import com.neovision.clinico.dto.CitaRequest;
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
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.service.EmailService;
import com.neovision.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el bug reportado: al editar una cita quitando el paciente (idPaciente
 * en null en el request), el bloque debia liberarse (volver a DISPONIBLE) en
 * vez de quedarse con el paciente anterior — antes el metodo solo actuaba
 * cuando idPaciente venia con un valor, e ignoraba en silencio el caso "sin
 * paciente".
 */
class CitaServiceImplEditarTest {

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CitaServiceImpl(citaRepository, sesionRepository, estudianteRepository,
                pacienteRepository, usuarioRepository, emailService, especialidadRepository,
                atencionRepository, metaPacientesSemestreRepository);
    }

    private Cita citaReservada() {
        Docente docente = new Docente();
        docente.setNombre("Jodan"); docente.setApellido("Aparicio");
        docente.setEspecialidades(Collections.<Especialidad>emptySet());

        SesionPractica sesion = new SesionPractica();
        sesion.setIdSesion(51); sesion.setFecha(LocalDate.of(2026, 9, 6)); sesion.setDocente(docente);

        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(7); estudiante.setNombre("Jose"); estudiante.setApellido("Navarro");

        Paciente paciente = new Paciente();
        paciente.setIdPaciente(8); paciente.setNombre("Daniela"); paciente.setApellido("Gomez");

        Cita cita = new Cita();
        cita.setIdCita(131);
        cita.setSesion(sesion);
        cita.setEstudiante(estudiante);
        cita.setPaciente(paciente);
        cita.setEstado("RESERVADO");
        return cita;
    }

    private CitaRequest requestSinPaciente(Cita cita) {
        CitaRequest req = new CitaRequest();
        req.setIdSesion(cita.getSesion().getIdSesion());
        req.setIdEstudiante(cita.getEstudiante().getIdEstudiante());
        req.setIdPaciente(null);
        return req;
    }

    @Test
    void quitarPaciente_liberaElBloque() {
        Cita cita = citaReservada();
        when(citaRepository.findById(131)).thenReturn(Optional.of(cita));
        when(estudianteRepository.findById(7)).thenReturn(Optional.of(cita.getEstudiante()));
        when(atencionRepository.existsByCitaIdCita(131)).thenReturn(false);
        when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editarCita(131, requestSinPaciente(cita));

        assertThat(cita.getPaciente()).isNull();
        assertThat(cita.getEstado()).isEqualTo("DISPONIBLE");
        assertThat(cita.getFechaReserva()).isNull();
        verify(citaRepository).save(cita);
    }

    @Test
    void quitarPaciente_siYaHayAtencion_seRechazaYNoSeToca() {
        Cita cita = citaReservada();
        when(citaRepository.findById(131)).thenReturn(Optional.of(cita));
        when(estudianteRepository.findById(7)).thenReturn(Optional.of(cita.getEstudiante()));
        when(atencionRepository.existsByCitaIdCita(131)).thenReturn(true);

        assertThatThrownBy(() -> service.editarCita(131, requestSinPaciente(cita)))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("atención de historia clínica asociada");

        // El estado de la cita no debe alterarse si la operacion se rechaza.
        assertThat(cita.getPaciente()).isNotNull();
        assertThat(cita.getEstado()).isEqualTo("RESERVADO");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void editarOtroCampo_conPacienteExistente_loConserva() {
        // Si el request SI trae idPaciente (el mismo que ya tenia, como pasa al
        // editar solo observaciones/estudiante desde el formulario), el paciente
        // se conserva y no se toca el estado.
        Cita cita = citaReservada();
        when(citaRepository.findById(131)).thenReturn(Optional.of(cita));
        when(estudianteRepository.findById(7)).thenReturn(Optional.of(cita.getEstudiante()));
        when(pacienteRepository.findById(8)).thenReturn(Optional.of(cita.getPaciente()));
        when(citaRepository.existeCitaReservada(8, "RESERVADO", 131)).thenReturn(false);
        when(citaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CitaRequest req = requestSinPaciente(cita);
        req.setIdPaciente(8);
        req.setObservaciones("Trae lentes de formula anterior");

        service.editarCita(131, req);

        assertThat(cita.getPaciente()).isNotNull();
        assertThat(cita.getEstado()).isEqualTo("RESERVADO");
        assertThat(cita.getObservaciones()).isEqualTo("Trae lentes de formula anterior");
    }
}
