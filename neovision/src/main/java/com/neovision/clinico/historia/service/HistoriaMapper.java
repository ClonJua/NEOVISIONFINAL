package com.neovision.clinico.historia.service;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.dto.AdendaResponse;
import com.neovision.clinico.historia.dto.AtencionResponse;
import com.neovision.clinico.historia.dto.AtencionResumenResponse;
import com.neovision.clinico.historia.dto.DiagnosticoDto;
import com.neovision.clinico.historia.dto.HistoriaClinicaResponse;
import com.neovision.clinico.historia.dto.ProcedimientoDto;
import com.neovision.clinico.historia.entity.Adenda;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.clinico.historia.entity.Diagnostico;
import com.neovision.clinico.historia.entity.HistoriaClinica;
import com.neovision.clinico.historia.entity.Procedimiento;
import org.springframework.stereotype.Component;

import java.util.List;

/** Conversión entidad → DTO para el módulo de historia clínica. */
@Component
public class HistoriaMapper {

    private static String nombreEstudiante(Atencion a) {
        return a.getEstudiante() == null ? null
                : a.getEstudiante().getNombre() + " " + a.getEstudiante().getApellido();
    }

    private static String nombreDocente(Atencion a) {
        return a.getDocenteSupervisor() == null ? null
                : a.getDocenteSupervisor().getNombre() + " " + a.getDocenteSupervisor().getApellido();
    }

    private static String nombrePaciente(Paciente p) {
        return p == null ? null : p.getNombre() + " " + p.getApellido();
    }

    public AtencionResumenResponse toResumen(Atencion a) {
        Paciente p = a.getHistoriaClinica() != null ? a.getHistoriaClinica().getPaciente() : null;
        return new AtencionResumenResponse(
                a.getIdAtencion(),
                a.getFechaAtencion(),
                a.getEstado().name(),
                p != null ? p.getIdPaciente() : null,
                nombrePaciente(p),
                nombreEstudiante(a),
                nombreDocente(a),
                a.getMotivoConsulta(),
                a.getDiagnosticos() == null ? 0 : a.getDiagnosticos().size()
        );
    }

    public AtencionResponse toAtencionResponse(Atencion a, List<Adenda> adendas) {
        AtencionResponse r = new AtencionResponse();
        Paciente p = a.getHistoriaClinica() != null ? a.getHistoriaClinica().getPaciente() : null;

        r.setIdAtencion(a.getIdAtencion());
        r.setIdHistoria(a.getHistoriaClinica() != null ? a.getHistoriaClinica().getIdHistoria() : null);
        r.setIdPaciente(p != null ? p.getIdPaciente() : null);
        r.setNombrePaciente(nombrePaciente(p));
        r.setIdCita(a.getCita() != null ? a.getCita().getIdCita() : null);
        r.setFechaCita(a.getCita() != null && a.getCita().getSesion() != null
                ? a.getCita().getSesion().getFecha().toString() : null);

        r.setEstado(a.getEstado().name());
        r.setFechaAtencion(a.getFechaAtencion());
        r.setFechaEnvioRevision(a.getFechaEnvioRevision());
        r.setFechaFinalizacion(a.getFechaFinalizacion());
        r.setMotivoDevolucion(a.getMotivoDevolucion());
        // Para que el estudiante vea hasta cuándo puede corregir, sin que el cliente
        // tenga que reimplementar (y desincronizar) la regla de la ventana.
        r.setLimiteEdicion(VentanaEdicion.limite(a));
        r.setEditablePorEstudiante(VentanaEdicion.editablePorEstudiante(a));

        r.setIdEstudiante(a.getEstudiante() != null ? a.getEstudiante().getIdEstudiante() : null);
        r.setNombreEstudiante(nombreEstudiante(a));
        r.setIdDocenteSupervisor(a.getDocenteSupervisor() != null ? a.getDocenteSupervisor().getIdDocente() : null);
        r.setNombreDocente(nombreDocente(a));

        r.setMotivoConsulta(a.getMotivoConsulta());
        r.setDatosGenerales(a.getDatosGenerales());
        r.setAnamnesis(a.getAnamnesis());
        r.setSignosVitales(a.getSignosVitales());
        r.setLensometria(a.getLensometria());
        r.setAgudezaVisual(a.getAgudezaVisual());
        r.setQueratometria(a.getQueratometria());
        r.setRefraccion(a.getRefraccion());
        r.setPruebasComplementarias(a.getPruebasComplementarias());
        r.setFormulaFinal(a.getFormulaFinal());
        r.setExamenExterno(a.getExamenExterno());
        r.setMotilidad(a.getMotilidad());
        r.setFondoOjo(a.getFondoOjo());
        r.setConducta(a.getConducta());

        r.setDiagnosticos(a.getDiagnosticos() == null ? List.of()
                : a.getDiagnosticos().stream().map(this::toDiagnosticoDto).toList());
        r.setProcedimientos(a.getProcedimientos() == null ? List.of()
                : a.getProcedimientos().stream().map(this::toProcedimientoDto).toList());

        r.setFirmaEstudiante(a.getFirmaEstudiante());
        r.setFirmaDocente(a.getFirmaDocente());
        r.setFirmaPaciente(a.getFirmaPaciente());
        r.setHashIntegridad(a.getHashIntegridad());

        r.setAdendas(adendas == null ? List.of()
                : adendas.stream().map(this::toAdendaResponse).toList());
        return r;
    }

    public DiagnosticoDto toDiagnosticoDto(Diagnostico d) {
        DiagnosticoDto dto = new DiagnosticoDto();
        dto.setCodigoCie10(d.getCodigoCie10());
        dto.setDescripcion(d.getDescripcion());
        dto.setOrden(d.getOrden());
        return dto;
    }

    public ProcedimientoDto toProcedimientoDto(Procedimiento p) {
        ProcedimientoDto dto = new ProcedimientoDto();
        dto.setCodigoCups(p.getCodigoCups());
        dto.setDescripcion(p.getDescripcion());
        dto.setOrden(p.getOrden());
        return dto;
    }

    public AdendaResponse toAdendaResponse(Adenda a) {
        return new AdendaResponse(a.getIdAdenda(), a.getTexto(), a.getAutorCorreo(), a.getAutorRol(), a.getFecha());
    }

    public HistoriaClinicaResponse toHistoriaResponse(HistoriaClinica h, Paciente paciente,
                                                      List<Atencion> atenciones, boolean tieneConsentimiento) {
        HistoriaClinicaResponse r = new HistoriaClinicaResponse();
        r.setIdHistoria(h != null ? h.getIdHistoria() : null);
        r.setIdPaciente(paciente != null ? paciente.getIdPaciente() : null);
        r.setNombrePaciente(nombrePaciente(paciente));
        r.setNumeroDocumento(paciente != null ? paciente.getNumeroDocumento() : null);
        r.setNumeroHistoria(h != null ? h.getNumeroHistoria() : null);
        r.setFechaApertura(h != null ? h.getFechaApertura() : null);
        r.setEstado(h != null ? h.getEstado() : null);
        r.setTieneConsentimiento(tieneConsentimiento);
        r.setAtenciones(atenciones == null ? List.of()
                : atenciones.stream().map(this::toResumen).toList());
        return r;
    }
}
