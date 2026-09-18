package com.neovision.clinico.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.neovision.clinico.dto.CitaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    // 15 columnas — deben coincidir exactamente con filaDesde()
    private static final String[] CABECERAS = {
        "ID", "Estado", "Fecha", "Hora Inicio", "Hora Fin",
        "Nombre Paciente", "Apellido Paciente", "Tipo Doc.", "Num. Documento",
        "Estudiante", "Docente", "Espacio", "Salon",
        "Observaciones", "Fecha Reserva"
    };

    // Colores para iText 5 (BaseColor)
    private static final BaseColor PDF_VERDE       = new BaseColor(0x1F, 0x7A, 0x63); // #1F7A63
    private static final BaseColor PDF_VERDE_CLARO = new BaseColor(0xE6, 0xF4, 0xEF); // #E6F4EF
    private static final BaseColor PDF_TEXTO       = new BaseColor(0x1C, 0x1C, 0x1E);
    private static final BaseColor PDF_BORDE       = new BaseColor(0xE5, 0xE7, 0xEB);
    private static final BaseColor PDF_SUBTITULO   = new BaseColor(0x6B, 0x72, 0x80);

    // Colores para Apache POI (java.awt.Color)
    private static final Color POI_VERDE       = new Color(0x1F, 0x7A, 0x63);
    private static final Color POI_VERDE_CLARO = new Color(0xE6, 0xF4, 0xEF);

    private final CitaService citaService;

    public ExportServiceImpl(CitaService citaService) {
        this.citaService = citaService;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    /**
     * Devuelve los valores de una cita en el mismo orden que CABECERAS (15 columnas).
     * Separa nombre y apellido del paciente para que cada campo quede en su columna.
     */
    private String[] filaDesde(CitaResponse c) {
        String nombreCompleto = nvl(c.getNombrePaciente());
        String apellido       = nvl(c.getApellidoPaciente());
        // nombreCompleto viene como "Nombre Apellido"; extraemos solo el nombre
        String nombreSolo = nombreCompleto;
        if (!apellido.isEmpty() && nombreCompleto.endsWith(" " + apellido)) {
            nombreSolo = nombreCompleto.substring(0, nombreCompleto.length() - apellido.length() - 1);
        }
        return new String[]{
            String.valueOf(c.getIdCita()),          // ID
            nvl(c.getEstado()),                     // Estado
            nvl(c.getFechaSesion()),                // Fecha
            nvl(c.getHoraInicio()),                 // Hora Inicio
            nvl(c.getHoraFin()),                    // Hora Fin
            nombreSolo,                             // Nombre Paciente
            apellido,                               // Apellido Paciente
            nvl(c.getTipoDocumentoPaciente()),      // Tipo Doc.
            nvl(c.getNumeroDocumentoPaciente()),    // Num. Documento
            nvl(c.getNombreEstudiante()),           // Estudiante
            nvl(c.getNombreDocente()),              // Docente
            nvl(c.getNombreEspacio()),              // Espacio
            nvl(c.getNombreSalon()),                // Salon
            nvl(c.getObservaciones()),              // Observaciones
            c.getFechaReserva() != null
                ? c.getFechaReserva().replace("T", " ") : ""  // Fecha Reserva
        };
    }

    // ------------------------------------------------------------------ CSV --

    @Override
    public byte[] exportarCSV(String estado) {
        List<CitaResponse> citas = citaService.listarCitas(estado);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // BOM UTF-8: Excel detecta la codificación y muestra tildes correctamente
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            pw.println(csvFila(CABECERAS));
            for (CitaResponse c : citas) {
                pw.println(csvFila(filaDesde(c)));
            }
        }
        return baos.toByteArray();
    }

    private String csvFila(String[] valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(',');
            // Cada campo va entre comillas; las comillas internas se duplican (RFC 4180)
            sb.append('"').append(sanitizarCsv(valores[i]).replace("\"", "\"\"")).append('"');
        }
        return sb.toString();
    }

    /**
     * Mitigación OWASP contra CSV Injection: si un campo empieza con un caracter
     * que Excel/Sheets interpreta como inicio de fórmula (=, +, -, @) al abrir el
     * CSV, se antepone un apóstrofo para forzar que se muestre como texto literal.
     * Necesario porque "observaciones" es texto libre que el paciente controla
     * (p. ej. al cancelar una cita) y termina en exportaciones que el admin abre.
     */
    private static String sanitizarCsv(String valor) {
        if (valor != null && !valor.isEmpty()) {
            char c = valor.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
                return "'" + valor;
            }
        }
        return valor;
    }

    // --------------------------------------------------------------- Excel --

    @Override
    public byte[] exportarExcel(String estado) {
        List<CitaResponse> citas = citaService.listarCitas(estado);
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Citas NeoVision");

            // Estilo de cabecera: fondo verde principal, texto blanco, negrita
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(
                    new XSSFColor(POI_VERDE, wb.getStylesSource().getIndexedColors()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(Color.WHITE, wb.getStylesSource().getIndexedColors()));
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);

            // Estilo de fila alternada: fondo verde claro
            XSSFCellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(
                    new XSSFColor(POI_VERDE_CLARO, wb.getStylesSource().getIndexedColors()));
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila de cabecera
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < CABECERAS.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(CABECERAS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowNum = 1;
            for (CitaResponse c : citas) {
                XSSFRow row = sheet.createRow(rowNum);
                XSSFCellStyle rowStyle = (rowNum % 2 == 0) ? altStyle : null;
                String[] fila = filaDesde(c);
                for (int i = 0; i < fila.length; i++) {
                    XSSFCell cell = row.createCell(i);
                    cell.setCellValue(fila[i]);
                    if (rowStyle != null) cell.setCellStyle(rowStyle);
                }
                rowNum++;
            }

            // Ajuste automático de ancho + padding
            for (int i = 0; i < CABECERAS.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
            }

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de citas", e);
            throw new RuntimeException("No se pudo generar el archivo Excel. Intenta de nuevo.");
        }
    }

    // ----------------------------------------------------------------- PDF --

    @Override
    public byte[] exportarPDF(String estado) {
        List<CitaResponse> citas = citaService.listarCitas(estado);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 horizontal para caber las 15 columnas
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Título
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, PDF_VERDE);
            Paragraph title = new Paragraph("Reporte de Citas — NeoVision", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            // Subtítulo con filtro y total
            String filtroTexto = (estado != null && !estado.isBlank()) ? estado : "Todas";
            Font subFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, PDF_SUBTITULO);
            doc.add(new Paragraph(
                    "Filtro: " + filtroTexto + "   |   Total registros: " + citas.size(), subFont));
            doc.add(new Paragraph(" "));

            // Tabla — 15 columnas con anchos proporcionales
            float[] colWidths = {
                1.5f, 3f, 3f, 2.5f, 2.5f,   // ID, Estado, Fecha, H.Ini, H.Fin
                3.5f, 3.5f, 2f,   3.5f,      // NomPac, ApePac, TipoDoc, NumDoc
                4f,   4f,   3.5f, 3f,         // Estudiante, Docente, Espacio, Salon
                4f,   4f                       // Observaciones, FechaReserva
            };
            PdfPTable table = new PdfPTable(colWidths);
            table.setWidthPercentage(100);
            table.setSpacingBefore(4f);

            // Celdas de cabecera
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.WHITE);
            for (String h : CABECERAS) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(PDF_VERDE);
                cell.setPadding(4);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(new BaseColor(0x14, 0x53, 0x2D));
                table.addCell(cell);
            }

            // Celdas de datos con filas alternas
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, PDF_TEXTO);
            boolean alt = false;
            for (CitaResponse c : citas) {
                BaseColor bg = alt ? PDF_VERDE_CLARO : BaseColor.WHITE;
                alt = !alt;
                for (String valor : filaDesde(c)) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor, dataFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(3);
                    cell.setBorderColor(PDF_BORDE);
                    table.addCell(cell);
                }
            }

            doc.add(table);
            doc.add(new Paragraph(" "));
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, PDF_SUBTITULO);
            doc.add(new Paragraph(
                    "Generado por el sistema NeoVision — Clínica de Optometría Universitaria",
                    footerFont));
            doc.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de citas", e);
            throw new RuntimeException("No se pudo generar el archivo PDF. Intenta de nuevo.");
        }
    }
}
