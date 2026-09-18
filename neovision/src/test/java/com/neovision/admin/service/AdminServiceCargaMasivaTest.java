package com.neovision.admin.service;

import com.neovision.admin.dto.CargaMasivaResponse;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
import com.neovision.clinico.repository.DocenteRepository;
import com.neovision.clinico.repository.EstudianteRepository;
import com.neovision.usuario.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integracion contra la BD local: ejercita el parseo del Excel (celdas
 * numericas y de texto, filas vacias), las validaciones fila a fila y la
 * transaccion por fila via el proxy. No usa @Transactional a nivel de test a
 * proposito, para reproducir el comportamiento real; limpia lo creado al final.
 */
@SpringBootTest
class AdminServiceCargaMasivaTest {

    @Autowired private AdminService adminService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DocenteRepository docenteRepository;
    @Autowired private EstudianteRepository estudianteRepository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private final String correoDocente = "carga." + sufijo + "@unbosque.edu.co";
    private final String correoEstudiante1 = "est1." + sufijo + "@unbosque.edu.co";
    private final String correoEstudiante2 = "est2." + sufijo + "@unbosque.edu.co";
    private final String cedulaEstudiante = "9" + sufijo.replaceAll("[^0-9]", "1").substring(0, 7) + "00";

    @AfterEach
    void limpiar() {
        docenteRepository.findByCorreo(correoDocente).ifPresent(docenteRepository::delete);
        for (String correo : List.of(correoEstudiante1, correoEstudiante2)) {
            estudianteRepository.findByCorreo(correo).ifPresent(estudianteRepository::delete);
        }
        for (String correo : List.of(correoDocente, correoEstudiante1, correoEstudiante2)) {
            usuarioRepository.findByCorreo(correo).ifPresent(usuarioRepository::delete);
        }
    }

    @Test
    void cargaDocentes_creaLasValidasYReportaLasDemas() throws IOException {
        // Fila 2: valida, con cedula NUMERICA (Excel la guarda asi) y una especialidad inexistente
        // Fila 3: correo no institucional -> error
        // Fila 4: vacia -> se ignora
        // Fila 5: sin nombre -> error de validacion del request
        byte[] excel = excel(
                new Object[]{"Ana", "Carga", correoDocente, 1099887766L, 3110000001L, "Ortoptica, Inventada"},
                new Object[]{"Luis", "Externo", "luis." + sufijo + "@gmail.com", "", "", ""},
                new Object[]{"", "", "", "", "", ""},
                new Object[]{"", "SinNombre", "sin." + sufijo + "@unbosque.edu.co", "", "", ""});

        CargaMasivaResponse r = adminService.cargarDocentesDesdeExcel(archivo(excel));

        assertEquals(3, r.getTotalFilas(), "la fila vacia no cuenta");
        assertEquals(1, r.getCreados());
        assertEquals(List.of(3, 5), r.getErrores().stream().map(CargaMasivaResponse.Incidencia::fila).toList());
        assertTrue(r.getErrores().get(0).mensaje().contains("institucional"));
        assertTrue(r.getErrores().get(1).mensaje().startsWith("nombre:"));
        assertEquals(1, r.getAdvertencias().size());
        assertEquals(2, r.getAdvertencias().get(0).fila());
        assertTrue(r.getAdvertencias().get(0).mensaje().contains("Inventada"));

        Integer idUsuario = usuarioRepository.findByCorreo(correoDocente).orElseThrow().getIdUsuario();
        Docente creado = docenteRepository.findWithEspecialidadesByUsuarioIdUsuario(idUsuario).orElseThrow();
        assertEquals("1099887766", creado.getCedula(), "la celda numerica llega sin .0");
        assertEquals("3110000001", creado.getTelefono());
        List<String> especialidades = creado.getEspecialidades().stream().map(e -> e.getNombre()).sorted().toList();
        assertEquals(List.of("Funcional", "Ortoptica"), especialidades);
    }

    @Test
    void cargaEstudiantes_validaSemestreYCedulaDuplicada() throws IOException {
        // Fila 2: valida (semestre numerico)
        // Fila 3: semestre 10 -> fuera de rango
        // Fila 4: misma cedula que la fila 2 -> duplicada (la fila 2 ya quedo creada)
        byte[] excel = excel(
                new Object[]{"Mia", "Uno", correoEstudiante1, cedulaEstudiante, 7, ""},
                new Object[]{"Leo", "Diez", correoEstudiante2, "1234567", 10, ""},
                new Object[]{"Dup", "Cedula", "dup." + sufijo + "@unbosque.edu.co", cedulaEstudiante, "8", ""});

        CargaMasivaResponse r = adminService.cargarEstudiantesDesdeExcel(archivo(excel));

        assertEquals(3, r.getTotalFilas());
        assertEquals(1, r.getCreados());
        assertEquals(List.of(3, 4), r.getErrores().stream().map(CargaMasivaResponse.Incidencia::fila).toList());
        assertTrue(r.getErrores().get(0).mensaje().startsWith("semestre:"));
        assertTrue(r.getErrores().get(1).mensaje().contains("ya está registrada"));

        Estudiante creado = estudianteRepository.findByCorreo(correoEstudiante1).orElseThrow();
        assertEquals(7, creado.getSemestre());
        assertFalse(usuarioRepository.existsByCorreo("dup." + sufijo + "@unbosque.edu.co"),
                "la fila con cedula duplicada no debe dejar un usuario huerfano");
    }

    @Test
    void cargaDocentes_rechazaCedulaOTelefonoNoNumericos() throws IOException {
        // Caso real reportado: al arrastrar una celda con formula (=D2+1) para
        // autocompletar cedulas de prueba, algunas filas quedan con el texto
        // literal de la formula en vez de un numero. Cada fila deja solo un
        // campo invalido para que el mensaje reportado sea deterministico.
        byte[] excel = excel(
                new Object[]{"Con", "FormulaTexto", correoDocente, "D2+1", "3110000001", ""},
                new Object[]{"Con", "TelefonoCorto", correoEstudiante1, "12345678", "123", ""});

        CargaMasivaResponse r = adminService.cargarDocentesDesdeExcel(archivo(excel));

        assertEquals(0, r.getCreados(), "ninguna fila con cedula/telefono no numerico debe crear el docente");
        assertEquals(2, r.getErrores().size());
        assertTrue(r.getErrores().get(0).mensaje().startsWith("cedula:"), r.getErrores().get(0).mensaje());
        assertTrue(r.getErrores().get(1).mensaje().startsWith("telefono:"), r.getErrores().get(1).mensaje());
        assertFalse(usuarioRepository.existsByCorreo(correoDocente));
    }

    @Test
    void archivoQueNoEsExcel_daMensajeLegible() {
        MockMultipartFile txt = new MockMultipartFile("archivo", "datos.txt", "text/plain", "hola".getBytes());
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> adminService.cargarDocentesDesdeExcel(txt));
        assertTrue(ex.getMessage().contains("Excel"));
    }

    @Test
    void plantillas_tienenCabeceraEInstrucciones() throws IOException {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(adminService.descargarPlantillaEstudiantes()))) {
            Sheet datos = wb.getSheetAt(0);
            assertEquals("Estudiantes", datos.getSheetName());
            assertEquals("Semestre", datos.getRow(0).getCell(4).getStringCellValue());
            assertEquals(1, datos.getPhysicalNumberOfRows(), "solo la cabecera: nada que se suba por accidente");
            assertEquals("Instrucciones", wb.getSheetAt(1).getSheetName());
        }
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(adminService.descargarPlantillaDocentes()))) {
            assertEquals("Especialidades", wb.getSheetAt(0).getRow(0).getCell(5).getStringCellValue());
        }
    }

    // --- helpers ---

    private static byte[] excel(Object[]... filas) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Datos");
            Row cabecera = sheet.createRow(0);
            for (int i = 0; i < 6; i++) cabecera.createCell(i).setCellValue("Col" + i);
            for (int f = 0; f < filas.length; f++) {
                Row row = sheet.createRow(f + 1);
                for (int c = 0; c < filas[f].length; c++) {
                    Object valor = filas[f][c];
                    if (valor instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                    else row.createCell(c).setCellValue(String.valueOf(valor));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "carga.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido);
    }
}
