package com.neovision.clinico.service;

import com.neovision.clinico.dto.SesionRequest;
import com.neovision.clinico.dto.SesionResponse;
import com.neovision.clinico.dto.CrearSesionRecurrenteRequest;
import com.neovision.clinico.dto.SesionRecurrenteResponse;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.entity.Salon;
import com.neovision.clinico.entity.SesionPractica;
import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.repository.CitaRepository;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.clinico.repository.SalonRepository;
import com.neovision.clinico.repository.SesionPracticaRepository;
import com.neovision.common.exception.DocenteNoEncontradoException;
import com.neovision.common.exception.EstadoInvalidoException;
import com.neovision.common.exception.SalonNoEncontradoException;
import com.neovision.common.exception.SesionNoEncontradaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SesionServiceImpl implements SesionService {

    private final SesionPracticaRepository sesionRepository;
    private final SalonRepository salonRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final CitaRepository citaRepository;

    public SesionServiceImpl(SesionPracticaRepository sesionRepository,
                             SalonRepository salonRepository,
                             DocenteRepository docenteRepository,
                             EstudianteRepository estudianteRepository,
                             CitaRepository citaRepository) {
        this.sesionRepository = sesionRepository;
        this.salonRepository = salonRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.citaRepository = citaRepository;
    }

    // Festivos fijos de Colombia que NUNCA se trasladan (mes-dia).
    private static final Set<MonthDay> FESTIVOS_FIJOS = Set.of(
            MonthDay.of(1, 1),   // Ano Nuevo
            MonthDay.of(5, 1),   // Dia del Trabajo
            MonthDay.of(7, 20),  // Grito de Independencia
            MonthDay.of(8, 7),   // Batalla de Boyaca
            MonthDay.of(12, 8),  // Inmaculada Concepcion
            MonthDay.of(12, 25)  // Navidad
    );

    // Festivos que la Ley Emiliani traslada al lunes siguiente si no caen en lunes
    // (mes-dia de la fecha nominal).
    private static final Set<MonthDay> FESTIVOS_TRASLADABLES = Set.of(
            MonthDay.of(1, 6),   // Reyes Magos
            MonthDay.of(3, 19),  // San Jose
            MonthDay.of(6, 29),  // San Pedro y San Pablo
            MonthDay.of(8, 15),  // Asuncion de la Virgen
            MonthDay.of(10, 12), // Dia de la Raza
            MonthDay.of(11, 1),  // Todos los Santos
            MonthDay.of(11, 11)  // Independencia de Cartagena
    );

    // Desplazamiento en dias desde el Domingo de Pascua para los festivos moviles
    // que tambien se trasladan al lunes siguiente por la Ley Emiliani.
    private static final int[] OFFSETS_TRASLADABLES_DESDE_PASCUA = { 39, 60, 68 }; // Ascension, Corpus Christi, Sagrado Corazon

    /**
     * Calcula el Domingo de Pascua para un año dado (algoritmo de Meeus/Jones/Butcher,
     * valido para el calendario gregoriano). Base para Semana Santa y los festivos
     * moviles que dependen de la Pascua.
     */
    private static LocalDate domingoDePascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }

    /** Festivos completos de Colombia para un año: fijos + trasladables (Ley Emiliani) + Semana Santa. */
    private static Set<LocalDate> festivosDelAnio(int anio) {
        Set<LocalDate> festivos = new HashSet<>();
        for (MonthDay md : FESTIVOS_FIJOS) {
            festivos.add(md.atYear(anio));
        }
        for (MonthDay md : FESTIVOS_TRASLADABLES) {
            festivos.add(trasladarALunes(md.atYear(anio)));
        }
        LocalDate pascua = domingoDePascua(anio);
        festivos.add(pascua.minusDays(3)); // Jueves Santo (no se traslada)
        festivos.add(pascua.minusDays(2)); // Viernes Santo (no se traslada)
        for (int offset : OFFSETS_TRASLADABLES_DESDE_PASCUA) {
            festivos.add(trasladarALunes(pascua.plusDays(offset)));
        }
        return festivos;
    }

    private static LocalDate trasladarALunes(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.MONDAY
                ? fecha
                : fecha.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private static final Map<String, String> DIAS_SEMANA_ES = Map.of(
            "MONDAY", "lunes", "TUESDAY", "martes", "WEDNESDAY", "miercoles",
            "THURSDAY", "jueves", "FRIDAY", "viernes", "SATURDAY", "sabado", "SUNDAY", "domingo");

    /** Traduce el enum DayOfWeek (en ingles) al mensaje que ve el admin; antes salia literal "Monday". */
    private String diaSemanaEnEspanol(String diaSemana) {
        return DIAS_SEMANA_ES.getOrDefault(diaSemana, diaSemana);
    }

    private boolean esFestivo(LocalDate fecha) {
        return festivosDelAnio(fecha.getYear()).contains(fecha);
    }

    /**
     * Reglas de fecha compartidas por crear y editar sesion. Antes solo
     * crearSesion() las aplicaba, asi que se podia editar una sesion existente
     * para moverla a un sabado, domingo, festivo o fecha pasada sin ninguna
     * validacion.
     */
    private static final int MAX_MESES_A_FUTURO = 6;

    private void validarHorarioYCupos(SesionPractica sesion) {
        if (sesion.getHoraInicio() != null && sesion.getHoraFin() != null
                && !sesion.getHoraFin().isAfter(sesion.getHoraInicio())) {
            throw new EstadoInvalidoException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (sesion.getCuposTotales() == null || sesion.getCuposTotales() < 1) {
            throw new EstadoInvalidoException("La sesión debe tener al menos un cupo");
        }
    }

    private void validarFechaSesion(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new EstadoInvalidoException("No se pueden programar sesiones en fechas pasadas");
        }
        if (fecha.isAfter(LocalDate.now().plusMonths(MAX_MESES_A_FUTURO))) {
            throw new EstadoInvalidoException("No se pueden programar sesiones con más de " + MAX_MESES_A_FUTURO + " meses de anticipación");
        }
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new EstadoInvalidoException("No se pueden programar sesiones los sábados ni domingos");
        }
        if (esFestivo(fecha)) {
            throw new EstadoInvalidoException("No se pueden programar sesiones en días festivos");
        }
    }

    @Override
    @Transactional
    public SesionResponse crearSesion(SesionRequest request) {
        LocalDate fecha = LocalDate.parse(request.getFecha());
        validarFechaSesion(fecha);

        Salon salon = null;
        if (request.getIdSalon() != null) {
            salon = salonRepository.findById(request.getIdSalon())
                    .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        }

        Docente docente = docenteRepository.findById(request.getIdDocente())
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));

        SesionPractica sesion = new SesionPractica();
        sesion.setSalon(salon);
        sesion.setDocente(docente);
        sesion.setFecha(fecha);
        if (request.getHoraInicio() != null && !request.getHoraInicio().isBlank()) {
            sesion.setHoraInicio(LocalTime.parse(request.getHoraInicio()));
        }
        if (request.getHoraFin() != null && !request.getHoraFin().isBlank()) {
            sesion.setHoraFin(LocalTime.parse(request.getHoraFin()));
        }
        sesion.setCuposTotales(request.getCuposTotales());
        validarHorarioYCupos(sesion);
        sesion.setActivo(true);

        if (request.getIdsEstudiantes() != null && !request.getIdsEstudiantes().isEmpty()) {
            List<Estudiante> estudiantes = estudianteRepository.findAllById(request.getIdsEstudiantes());
            sesion.setEstudiantes(estudiantes);
        }

        sesionRepository.save(sesion);
        generarBloquesAutomaticos(sesion);
        return toResponse(sesion);
    }

    /**
     * Antes, crear una sesion con "cupos" no generaba ninguna cita real: el
     * admin tenia que ir a la pestana de Citas y crear cada bloque a mano, uno
     * por uno, sin que nada avisara si se quedaba corto. Ahora los cupos se
     * traducen de inmediato en bloques DISPONIBLE reales.
     *
     * <p>En la practica clinica un paciente es atendido normalmente por DOS
     * estudiantes a la vez, asi que cada bloque agrupa a una pareja de
     * estudiantes consecutivos de la lista de la sesion (si son impares, el
     * ultimo queda solo). El limite real de bloques autogenerados es el numero
     * de parejas, no los cupos pedidos. Si sobran cupos, el admin los ve
     * reflejados en "cupos disponibles" y puede crear el resto a mano en Citas
     * (o agregar mas estudiantes a la sesion); la pareja de cada bloque se
     * puede ajustar en "Editar cita".
     *
     * @return numero de bloques creados.
     */
    private int generarBloquesAutomaticos(SesionPractica sesion) {
        List<Estudiante> estudiantes = sesion.getEstudiantes();
        if (estudiantes == null || estudiantes.isEmpty()) return 0;

        int parejas = (estudiantes.size() + 1) / 2;
        int cantidad = Math.min(sesion.getCuposTotales(), parejas);
        for (int i = 0; i < cantidad; i++) {
            Cita cita = new Cita();
            cita.setSesion(sesion);
            cita.setEstudiante(estudiantes.get(2 * i));
            if (2 * i + 1 < estudiantes.size()) {
                cita.setEstudiante2(estudiantes.get(2 * i + 1));
            }
            cita.setEstado("DISPONIBLE");
            citaRepository.save(cita);
        }
        return cantidad;
    }

    @Override
    @Transactional
    public SesionRecurrenteResponse crearSesionesRecurrentes(CrearSesionRecurrenteRequest request) {
        LocalDate fechaActual = LocalDate.now();

        if (request.getFechaInicio().isBefore(fechaActual)) {
            throw new EstadoInvalidoException("La fecha de inicio no puede ser en el pasado");
        }

        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new EstadoInvalidoException("La fecha fin debe ser posterior a la fecha de inicio");
        }

        if (request.getFechaFin().isAfter(fechaActual.plusMonths(MAX_MESES_A_FUTURO))) {
            throw new EstadoInvalidoException("No se pueden programar sesiones con más de " + MAX_MESES_A_FUTURO + " meses de anticipación");
        }

        DayOfWeek targetDay = DayOfWeek.valueOf(request.getDiaSemana());

        Salon salon = null;
        if (request.getIdSalon() != null) {
            salon = salonRepository.findById(request.getIdSalon())
                    .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        }

        Docente docente = null;
        if (request.getIdDocente() != null) {
            docente = docenteRepository.findById(request.getIdDocente())
                    .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));
        }

        List<Estudiante> estudiantes = (request.getIdsEstudiantes() != null && !request.getIdsEstudiantes().isEmpty())
                ? estudianteRepository.findAllById(request.getIdsEstudiantes())
                : List.of();

        List<SesionPractica> sesionesCreadas = new ArrayList<>();
        LocalDate fecha = request.getFechaInicio();
        int bloquesCreados = 0;

        while (!fecha.isAfter(request.getFechaFin())) {
            if (fecha.getDayOfWeek() == targetDay && !esFestivo(fecha)) {
                SesionPractica sesion = new SesionPractica();
                sesion.setSalon(salon);
                sesion.setDocente(docente);
                sesion.setFecha(fecha);
                sesion.setHoraInicio(request.getHoraInicio());
                sesion.setHoraFin(request.getHoraFin());
                sesion.setCuposTotales(request.getCuposTotales());
                sesion.setActivo(true);
                if (!estudiantes.isEmpty()) {
                    sesion.setEstudiantes(new ArrayList<>(estudiantes));
                }
                sesionRepository.save(sesion);
                bloquesCreados += generarBloquesAutomaticos(sesion);
                sesionesCreadas.add(sesion);
            }
            fecha = fecha.plusDays(1);
        }

        String mensaje = String.format("Se crearon %d sesiones recurrentes para los %s de %s a %s, con %d bloques disponibles generados automaticamente",
                sesionesCreadas.size(), diaSemanaEnEspanol(request.getDiaSemana()),
                request.getFechaInicio(), request.getFechaFin(), bloquesCreados);

        return new SesionRecurrenteResponse(sesionesCreadas.size(), mensaje);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionResponse> listarSesiones() {
        return sesionRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionResponse> listarSesionesPorFecha(String fecha) {
        return sesionRepository.findByFechaAndActivoTrue(LocalDate.parse(fecha)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SesionResponse editarSesion(Integer id, SesionRequest request) {
        SesionPractica sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new SesionNoEncontradaException("Sesion no encontrada"));

        Salon salon = null;
        if (request.getIdSalon() != null) {
            salon = salonRepository.findById(request.getIdSalon())
                    .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        }
        Docente docente = docenteRepository.findById(request.getIdDocente())
                .orElseThrow(() -> new DocenteNoEncontradoException("Docente no encontrado"));

        sesion.setSalon(salon);
        sesion.setDocente(docente);
        if (request.getFecha() != null) {
            LocalDate nuevaFecha = LocalDate.parse(request.getFecha());
            validarFechaSesion(nuevaFecha);
            sesion.setFecha(nuevaFecha);
        }
        if (request.getHoraInicio() != null && !request.getHoraInicio().isBlank()) {
            sesion.setHoraInicio(LocalTime.parse(request.getHoraInicio()));
        }
        if (request.getHoraFin() != null && !request.getHoraFin().isBlank()) {
            sesion.setHoraFin(LocalTime.parse(request.getHoraFin()));
        }
        sesion.setCuposTotales(request.getCuposTotales());
        validarHorarioYCupos(sesion);

        if (request.getIdsEstudiantes() != null) {
            List<Estudiante> estudiantes = estudianteRepository.findAllById(request.getIdsEstudiantes());
            sesion.setEstudiantes(estudiantes);
        }

        sesionRepository.save(sesion);
        return toResponse(sesion);
    }

    @Override
    @Transactional
    public void desactivarSesion(Integer id) {
        SesionPractica sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new SesionNoEncontradaException("Sesion no encontrada"));
        sesion.setActivo(false);
        sesionRepository.save(sesion);
    }

    @Override
    @Transactional
    public void eliminarSesion(Integer id) {
        SesionPractica sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new SesionNoEncontradaException("Sesion no encontrada"));
        try {
            sesion.getEstudiantes().clear();
            sesionRepository.save(sesion);
            sesionRepository.delete(sesion);
            sesionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: la sesión tiene citas asociadas. Desactívala en su lugar.");
        }
    }

    private SesionResponse toResponse(SesionPractica sesion) {
        List<SesionResponse.EstudianteInfo> estudiantesInfo = sesion.getEstudiantes().stream()
                .map(e -> new SesionResponse.EstudianteInfo(e.getIdEstudiante(), e.getNombre() + " " + e.getApellido()))
                .toList();

        // Calcular cupos disponibles reales: cuposTotales - citas activas (DISPONIBLE o RESERVADO)
        long citasActivas = citaRepository.findBySesionIdSesion(sesion.getIdSesion()).stream()
                .filter(c -> "DISPONIBLE".equals(c.getEstado()) || "RESERVADO".equals(c.getEstado()))
                .count();
        int cuposDisponibles = Math.max(0, sesion.getCuposTotales() - (int) citasActivas);

        Salon salon = sesion.getSalon();
        return new SesionResponse(
                sesion.getIdSesion(),
                sesion.getFecha().toString(),
                sesion.getHoraInicio() != null ? sesion.getHoraInicio().toString() : null,
                sesion.getHoraFin() != null ? sesion.getHoraFin().toString() : null,
                sesion.getCuposTotales(),
                cuposDisponibles,
                sesion.getActivo(),
                salon != null ? salon.getIdSalon() : null,
                salon != null ? salon.getNombre() : null,
                salon != null ? salon.getEspacio().getNombre() : null,
                sesion.getDocente().getIdDocente(),
                sesion.getDocente().getNombre() + " " + sesion.getDocente().getApellido(),
                estudiantesInfo
        );
    }
}
