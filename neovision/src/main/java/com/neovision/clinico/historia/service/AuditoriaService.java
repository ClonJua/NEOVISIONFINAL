package com.neovision.clinico.historia.service;

import com.neovision.clinico.historia.dto.AuditoriaResponse;
import com.neovision.clinico.historia.dto.VerificacionCadenaResponse;
import com.neovision.clinico.historia.entity.Auditoria;
import com.neovision.clinico.historia.enums.AccionAuditoria;
import com.neovision.clinico.historia.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro append-only de la bitácora de auditoría. Solo expone escritura de
 * nuevos eventos; nunca actualiza ni elimina, garantizando la trazabilidad
 * inalterable de las acciones sobre la historia clínica.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String usuarioCorreo, String rol, AccionAuditoria accion,
                          String entidad, Integer entidadId, Integer idPaciente, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuarioCorreo(usuarioCorreo);
        a.setRol(rol);
        a.setAccion(accion);
        a.setEntidad(entidad);
        a.setEntidadId(entidadId);
        a.setIdPaciente(idPaciente);
        a.setFechaHora(LocalDateTime.now());
        a.setDetalle(detalle);

        String[] origen = clientInfo();
        a.setIp(origen[0]);
        a.setUserAgent(origen[1]);

        // Encadenado: el hash de este registro incluye el del anterior. Alterar o
        // borrar cualquier evento rompe la cadena y se detecta al verificarla.
        String hashPrevio = auditoriaRepository.findTopByOrderByIdAuditoriaDesc()
                .map(Auditoria::getHashRegistro).orElse("GENESIS");
        a.setHashPrevio(hashPrevio);
        a.setHashRegistro(calcularHashRegistro(a, hashPrevio));

        auditoriaRepository.save(a);
    }

    /** Hash SHA-256 del contenido canónico del evento, encadenado al anterior. */
    private String calcularHashRegistro(Auditoria a, String hashPrevio) {
        String canonico = String.join("|",
                str(a.getUsuarioCorreo()), str(a.getRol()), a.getAccion().name(),
                str(a.getEntidad()), str(a.getEntidadId()), str(a.getIdPaciente()),
                a.getFechaHora().toString(), str(a.getDetalle()),
                str(a.getIp()), str(a.getUserAgent()), hashPrevio);
        return HashUtil.sha256Hex(canonico);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** IP y user-agent del request actual (si hay contexto HTTP). */
    private String[] clientInfo() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
                else ip = ip.split(",")[0].trim();
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.length() > 255) ua = ua.substring(0, 255);
                return new String[]{ip, ua};
            }
        } catch (Exception ignored) {
            // Sin contexto HTTP (p. ej. tarea programada): se registra sin origen.
        }
        return new String[]{null, null};
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> consultarPorPaciente(Integer idPaciente) {
        return auditoriaRepository.findByIdPacienteOrderByFechaHoraDesc(idPaciente).stream()
                .map(a -> new AuditoriaResponse(
                        a.getIdAuditoria(), a.getUsuarioCorreo(), a.getRol(), a.getAccion().name(),
                        a.getEntidad(), a.getEntidadId(), a.getFechaHora(), a.getDetalle()))
                .toList();
    }

    /**
     * Recorre toda la bitácora en orden y recalcula cada hash encadenado,
     * comparándolo con el almacenado. La construcción del hash (registrar())
     * ya lo hacía; sin esto no había forma de comprobar que la cadena no fue
     * alterada — solo de generarla.
     */
    @Transactional(readOnly = true)
    public VerificacionCadenaResponse verificarCadena() {
        List<Auditoria> todos = auditoriaRepository.findAllByOrderByIdAuditoriaAsc();
        String hashPrevio = "GENESIS";
        for (Auditoria a : todos) {
            if (!hashPrevio.equals(a.getHashPrevio())) {
                return new VerificacionCadenaResponse(false, todos.size(), a.getIdAuditoria(),
                        "Cadena rota: el registro #" + a.getIdAuditoria() + " no enlaza con el anterior.");
            }
            String recalculado = calcularHashRegistro(a, hashPrevio);
            if (!recalculado.equals(a.getHashRegistro())) {
                return new VerificacionCadenaResponse(false, todos.size(), a.getIdAuditoria(),
                        "Registro alterado: el contenido del evento #" + a.getIdAuditoria()
                        + " no coincide con su sello original.");
            }
            hashPrevio = a.getHashRegistro();
        }
        return new VerificacionCadenaResponse(true, todos.size(), null,
                "Cadena íntegra: " + todos.size() + " registros verificados sin alteraciones.");
    }
}
