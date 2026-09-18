package com.neovision.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC08–TC09: Flujo del estudiante — ver bloques y asignar paciente.
 * La confirmacion de asistencia (antes TC10 aqui) ahora la hace el docente
 * responsable de la sesion, no el estudiante — ver {@link DocenteTest}.
 */
@DisplayName("Estudiante: Bloques y asignacion de paciente")
class EstudianteTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC08 - Estudiante ve sus bloques asignados")
    void verMisBloques() {
        loginInstitucional(ESTUDIANTE_CORREO, ESTUDIANTE_PASS);

        // El dashboard del estudiante muestra tabs de sus bloques
        clickTab("Agendadas");

        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("bloque") || body.contains("cita") || body.contains("no hay") || body.contains("sesión");
        });

        // La página debe cargar sin errores
        String body = driver.findElement(By.tagName("body")).getText();
        assertFalse(body.toLowerCase().contains("error de servidor"),
                "No debe haber error de servidor al cargar los bloques del estudiante");
    }

    @Test
    @DisplayName("TC09 - Estudiante asigna un paciente a un bloque disponible")
    void asignarPacienteABloque() {
        loginInstitucional(ESTUDIANTE_CORREO, ESTUDIANTE_PASS);

        // Los bloques sin paciente asignado aparecen en "Disponibles" para el estudiante
        clickTab("Disponibles");

        WebElement botonAsignar = waitForClickable(
                By.xpath("//button[contains(.,'Asignar paciente') or contains(.,'Asignar')]")
        );
        botonAsignar.click();

        // Debe aparecer un modal o sección para seleccionar paciente
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("paciente") && (body.contains("seleccionar") || body.contains("buscar") || body.contains("lista"));
        });

        // Seleccionar el primer paciente de la lista
        WebElement primerPaciente = waitForClickable(
                By.xpath("//button[contains(@class,'paciente') or contains(.,'Seleccionar')]")
        );
        primerPaciente.click();

        // Verificar que el bloque ahora muestra el nombre del paciente asignado
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("asignado") || body.contains("paciente asignado");
        });

        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertTrue(body.contains("asignado") || body.contains("reservado"),
                "El bloque debe reflejar que tiene un paciente asignado");
    }
}
