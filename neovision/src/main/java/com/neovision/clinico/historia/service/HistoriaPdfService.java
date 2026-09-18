package com.neovision.clinico.historia.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.neovision.clinico.historia.dto.AtencionResponse;
import com.neovision.clinico.historia.dto.DiagnosticoDto;
import com.neovision.clinico.historia.dto.ProcedimientoDto;
import com.neovision.clinico.historia.entity.Atencion;
import com.neovision.common.exception.AtencionNoEncontradaException;
import com.neovision.clinico.historia.enums.AccionAuditoria;
import com.neovision.clinico.historia.enums.EstadoAtencion;
import com.neovision.clinico.historia.repository.AdendaRepository;
import com.neovision.clinico.historia.repository.AtencionRepository;
import com.neovision.clinico.historia.repository.HistoriaClinicaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;

/**
 * Genera el PDF de una atención clínica (Examen Funcional Optométrico, 4 folios)
 * usando iText 5, reutilizando la librería ya presente en el proyecto. Recorre
 * las secciones por reflexión para volcar todos los campos diligenciados.
 */
@Service
public class HistoriaPdfService {

    private static final Logger log = LoggerFactory.getLogger(HistoriaPdfService.class);

    private static final BaseColor AZUL    = new BaseColor(0x00, 0x78, 0xD4);
    private static final BaseColor GRIS    = new BaseColor(0x6B, 0x72, 0x80);
    private static final BaseColor BORDE   = new BaseColor(0xE5, 0xE7, 0xEB);
    private static final BaseColor FONDO   = new BaseColor(0xF3, 0xF4, 0xF6);

    private final AtencionRepository atencionRepository;
    private final AdendaRepository adendaRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final HistoriaMapper mapper;
    private final AuditoriaService auditoriaService;
    private final HistoriaAccessGuard accessGuard;

    public HistoriaPdfService(AtencionRepository atencionRepository, AdendaRepository adendaRepository,
                              HistoriaClinicaRepository historiaRepository,
                              HistoriaMapper mapper, AuditoriaService auditoriaService,
                              HistoriaAccessGuard accessGuard) {
        this.atencionRepository = atencionRepository;
        this.adendaRepository = adendaRepository;
        this.historiaRepository = historiaRepository;
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public byte[] generar(Integer idAtencion, Authentication auth) {
        Atencion atencion = atencionRepository.findById(idAtencion)
                .orElseThrow(() -> new AtencionNoEncontradaException("Atención no encontrada"));
        accessGuard.requireLecturaAtencion(atencion, auth);
        AtencionResponse a = mapper.toAtencionResponse(atencion,
                adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(idAtencion));

        Integer idPaciente = atencion.getHistoriaClinica() != null && atencion.getHistoriaClinica().getPaciente() != null
                ? atencion.getHistoriaClinica().getPaciente().getIdPaciente() : null;
        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.CONSULTAR, "ATENCION", idAtencion, idPaciente, "Exportación a PDF");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Encabezado
            Font titulo = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, AZUL);
            Paragraph t = new Paragraph("Examen Funcional Optométrico", titulo);
            t.setAlignment(Element.ALIGN_CENTER);
            doc.add(t);
            Font sub = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRIS);
            Paragraph s = new Paragraph("NeoVision — Clínica de Optometría, Universidad El Bosque", sub);
            s.setAlignment(Element.ALIGN_CENTER);
            s.setSpacingAfter(8f);
            doc.add(s);

            renderAtencion(doc, a);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de la historia clínica", e);
            throw new RuntimeException("No se pudo generar el PDF de la historia clínica. Intenta de nuevo.");
        }
    }

    /**
     * Genera un PDF con TODAS las atenciones finalizadas del paciente (copia de su
     * historia clínica). Usado al aprobar una solicitud de copia.
     */
    @Transactional
    public byte[] generarPorPaciente(Integer idPaciente, Authentication auth) {
        var historia = historiaRepository.findByPacienteIdPaciente(idPaciente).orElse(null);
        List<Atencion> finalizadas = historia == null ? List.of()
                : atencionRepository.findByHistoriaClinicaIdHistoriaOrderByFechaAtencionDesc(historia.getIdHistoria())
                    .stream().filter(at -> at.getEstado() == EstadoAtencion.FINALIZADA).toList();

        auditoriaService.registrar(auth.getName(), HistoriaClinicaService.rol(auth),
                AccionAuditoria.CONSULTAR, "HISTORIA",
                historia != null ? historia.getIdHistoria() : null, idPaciente,
                "Generación de copia de historia clínica (solicitud)");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, AZUL);
            Paragraph t = new Paragraph("Historia Clínica — Copia", titulo);
            t.setAlignment(Element.ALIGN_CENTER);
            doc.add(t);
            Font sub = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRIS);
            Paragraph s = new Paragraph("NeoVision — Clínica de Optometría, Universidad El Bosque", sub);
            s.setAlignment(Element.ALIGN_CENTER);
            s.setSpacingAfter(10f);
            doc.add(s);

            if (finalizadas.isEmpty()) {
                doc.add(new Paragraph("El paciente no tiene atenciones finalizadas registradas.",
                        new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, GRIS)));
            } else {
                boolean primera = true;
                for (Atencion at : finalizadas) {
                    if (!primera) doc.newPage();
                    primera = false;
                    AtencionResponse a = mapper.toAtencionResponse(at,
                            adendaRepository.findByAtencionIdAtencionOrderByFechaAsc(at.getIdAtencion()));
                    Paragraph st = new Paragraph("Examen Funcional Optométrico — " + str(a.getFechaAtencion()),
                            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, AZUL));
                    st.setSpacingAfter(6f);
                    doc.add(st);
                    renderAtencion(doc, a);
                }
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando copia de la historia clínica", e);
            throw new RuntimeException("No se pudo generar la copia de la historia clínica. Intenta de nuevo.");
        }
    }

    /** Renderiza el cuerpo de una atención (metadatos + folios + firmas + adendas). */
    private void renderAtencion(Document doc, AtencionResponse a) throws DocumentException {
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(8f);
        metaFila(meta, "Paciente", a.getNombrePaciente());
        metaFila(meta, "Estado", a.getEstado());
        metaFila(meta, "Fecha de atención", str(a.getFechaAtencion()));
        metaFila(meta, "Estudiante", a.getNombreEstudiante());
        metaFila(meta, "Docente supervisor", a.getNombreDocente());
        metaFila(meta, "Finalización", str(a.getFechaFinalizacion()));
        doc.add(meta);

        if (a.getMotivoConsulta() != null && !a.getMotivoConsulta().isBlank()) {
            seccionTitulo(doc, "Motivo de consulta");
            doc.add(new Paragraph(a.getMotivoConsulta(), new Font(Font.FontFamily.HELVETICA, 9)));
        }

        seccion(doc, "Datos generales", a.getDatosGenerales());
        seccion(doc, "Anamnesis y antecedentes", a.getAnamnesis());
        seccion(doc, "Signos vitales", a.getSignosVitales());
        seccion(doc, "Lensometría", a.getLensometria());
        seccion(doc, "Agudeza visual", a.getAgudezaVisual());
        seccion(doc, "Queratometría", a.getQueratometria());
        seccion(doc, "Refracción", a.getRefraccion());
        seccion(doc, "Pruebas complementarias", a.getPruebasComplementarias());
        seccion(doc, "Fórmula final", a.getFormulaFinal());
        seccion(doc, "Examen externo / Biomicroscopía", a.getExamenExterno());
        imagenes(doc, a.getExamenExterno(), "Diagramas del segmento anterior");
        seccion(doc, "Motilidad", a.getMotilidad());
        imagenes(doc, a.getMotilidad(), "Diagrama de versiones");
        seccion(doc, "Fondo de ojo", a.getFondoOjo());
        imagenes(doc, a.getFondoOjo(), "Diagramas del fondo de ojo");
        seccion(doc, "Conducta", a.getConducta());

        if (a.getDiagnosticos() != null && !a.getDiagnosticos().isEmpty()) {
            seccionTitulo(doc, "Diagnósticos (CIE-10)");
            Font df = new Font(Font.FontFamily.HELVETICA, 9);
            for (DiagnosticoDto d : a.getDiagnosticos()) {
                doc.add(new Paragraph("• " + nvl(d.getCodigoCie10()) + "  " + nvl(d.getDescripcion()), df));
            }
        }

        if (a.getProcedimientos() != null && !a.getProcedimientos().isEmpty()) {
            seccionTitulo(doc, "Procedimientos (CUPS)");
            Font pf = new Font(Font.FontFamily.HELVETICA, 9);
            for (ProcedimientoDto p : a.getProcedimientos()) {
                doc.add(new Paragraph("• " + nvl(p.getCodigoCups()) + "  " + nvl(p.getDescripcion()), pf));
            }
        }

        seccionTitulo(doc, "Firmas");
        PdfPTable firmas = new PdfPTable(3);
        firmas.setWidthPercentage(100);
        firmaCelda(firmas, "Estudiante", a.getFirmaEstudiante());
        firmaCelda(firmas, "Docente", a.getFirmaDocente());
        firmaCelda(firmas, "Paciente", a.getFirmaPaciente());
        doc.add(firmas);

        if (a.getHashIntegridad() != null) {
            Font hf = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, GRIS);
            Paragraph h = new Paragraph("Sello de integridad (SHA-256): " + a.getHashIntegridad(), hf);
            h.setSpacingBefore(8f);
            doc.add(h);
        }

        if (a.getAdendas() != null && !a.getAdendas().isEmpty()) {
            seccionTitulo(doc, "Adendas");
            Font af = new Font(Font.FontFamily.HELVETICA, 9);
            Font am = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, GRIS);
            for (var ad : a.getAdendas()) {
                doc.add(new Paragraph(ad.getTexto(), af));
                doc.add(new Paragraph(nvl(ad.getAutorCorreo()) + " · " + str(ad.getFecha()), am));
            }
        }
    }

    // ---- helpers ----

    private void seccionTitulo(Document doc, String titulo) throws DocumentException {
        Font f = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, AZUL);
        Paragraph p = new Paragraph(titulo, f);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(3f);
        doc.add(p);
    }

    /** Vuelca por reflexión los campos no vacíos de una sección como tabla label/valor. */
    private void seccion(Document doc, String titulo, Object obj) throws DocumentException {
        if (obj == null) return;
        PdfPTable table = new PdfPTable(new float[]{ 2.2f, 3.8f });
        table.setWidthPercentage(100);
        Font lf = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(0x37, 0x41, 0x51));
        Font vf = new Font(Font.FontFamily.HELVETICA, 8);
        boolean hay = false;
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object val;
            try { val = f.get(obj); } catch (IllegalAccessException e) { continue; }
            if (val == null) continue;
            String name = f.getName();
            if (name.toLowerCase().startsWith("diagrama")) continue; // imágenes aparte
            String texto;
            if (val instanceof Boolean b) {
                if (!b) continue;          // omitir checkboxes en false
                texto = "Sí";
            } else {
                texto = String.valueOf(val);
                if (texto.isBlank()) continue;
            }
            hay = true;
            PdfPCell c1 = new PdfPCell(new Phrase(humanizar(name), lf));
            c1.setBackgroundColor(FONDO); c1.setPadding(3); c1.setBorderColor(BORDE);
            PdfPCell c2 = new PdfPCell(new Phrase(texto, vf));
            c2.setPadding(3); c2.setBorderColor(BORDE);
            table.addCell(c1); table.addCell(c2);
        }
        if (!hay) return;
        seccionTitulo(doc, titulo);
        doc.add(table);
    }

    /** Embebe los diagramas (campos diagramaOd/diagramaOi) de una sección. */
    private void imagenes(Document doc, Object obj, String titulo) {
        if (obj == null) return;
        try {
            Image od = imagenDe(obj, "diagramaOd");
            Image oi = imagenDe(obj, "diagramaOi");
            if (od == null && oi == null) return;
            seccionTitulo(doc, titulo);
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(80);
            t.setHorizontalAlignment(Element.ALIGN_LEFT);
            t.addCell(celdaImagen(od, "OD"));
            t.addCell(celdaImagen(oi, "OI"));
            doc.add(t);
        } catch (Exception ignored) { /* el PDF no debe fallar por una imagen */ }
    }

    private Image imagenDe(Object obj, String campo) {
        try {
            Field f = obj.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? null : imagenBase64(String.valueOf(v));
        } catch (Exception e) { return null; }
    }

    private Image imagenBase64(String dataUrl) {
        try {
            if (dataUrl == null || dataUrl.isBlank()) return null;
            String b64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(',') + 1) : dataUrl;
            Image img = Image.getInstance(Base64.getDecoder().decode(b64));
            img.scaleToFit(220, 150);
            return img;
        } catch (Exception e) { return null; }
    }

    private PdfPCell celdaImagen(Image img, String label) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDE);
        c.setPadding(4);
        Font lf = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, GRIS);
        c.addElement(new Phrase(label, lf));
        if (img != null) c.addElement(img);
        else c.addElement(new Phrase("Sin diagrama", lf));
        return c;
    }

    private void firmaCelda(PdfPTable table, String label, String firma) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDE);
        c.setPadding(6);
        c.setMinimumHeight(60);
        Image img = imagenBase64(firma);
        if (img != null) { img.scaleToFit(140, 50); c.addElement(img); }
        Font lf = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, GRIS);
        c.addElement(new Phrase("Firma " + label, lf));
        table.addCell(c);
    }

    private void metaFila(PdfPTable t, String label, String valor) {
        Font lf = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(0x37, 0x41, 0x51));
        Font vf = new Font(Font.FontFamily.HELVETICA, 8);
        PdfPCell c1 = new PdfPCell(new Phrase(label, lf));
        c1.setBackgroundColor(FONDO); c1.setPadding(3); c1.setBorderColor(BORDE);
        PdfPCell c2 = new PdfPCell(new Phrase(nvl(valor), vf));
        c2.setPadding(3); c2.setBorderColor(BORDE);
        t.addCell(c1); t.addCell(c2);
    }

    /** odEsfera -> "Od esfera"; camelCase a etiqueta legible. */
    private String humanizar(String campo) {
        String conEspacios = campo.replaceAll("([a-z])([A-Z0-9])", "$1 $2");
        return Character.toUpperCase(conEspacios.charAt(0)) + conEspacios.substring(1);
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    private static String str(Object o) { return o != null ? o.toString().replace("T", " ") : ""; }
}
