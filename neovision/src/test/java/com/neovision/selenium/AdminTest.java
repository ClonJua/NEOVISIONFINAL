package com.neovision.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TC11–TC14: Flujo del administrador — acceso al panel, CRUD de usuarios y sesiones.
 */
@DisplayName("Admin: Panel de administración")
class AdminTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC11 - Admin accede al panel de administración")
    void accesoPanelAdmin() {
        loginInstitucional(ADMIN_CORREO, ADMIN_PASS);

        // El panel admin tiene sidebar con tabs
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("docentes") || body.contains("estudiantes") || body.contains("sesiones");
        });

        // Verificar que el botón de logout es visible (usuario autenticado)
        assertTrue(isVisible(By.cssSelector("[aria-label='Cerrar sesion']")),
                "El panel admin debe mostrar opción de cerrar sesión");

        // Verificar que hay tabs de navegación
        String body = driver.findElement(By.tagName("body")).getText();
        assertTrue(body.contains("Docentes") || body.contains("Estudiantes") || body.contains("Sesiones"),
                "El panel admin debe mostrar las secciones principales");
    }

    @Test
    @DisplayName("TC12 - Admin crea un nuevo docente")
    void crearDocente() {
        loginInstitucional(ADMIN_CORREO, ADMIN_PASS);

        // Navegar al tab de Docentes
        clickTab("Docentes");

        // Abrir formulario de creación
        WebElement botonNuevo = waitForClickable(
                By.xpath("//button[contains(.,'Nuevo') or contains(.,'Agregar') or contains(.,'Crear')]")
        );
        botonNuevo.click();

        // Llenar formulario
        waitFor(By.name("nombre")).sendKeys("Pedro");
        driver.findElement(By.name("apellido")).sendKeys("Test");

        String correoUnico = "docente.test." + System.currentTimeMillis() + "@unbosque.edu.co";
        driver.findElement(By.name("correo")).sendKeys(correoUnico);
        driver.findElement(By.name("cedula")).sendKeys("11" + System.currentTimeMillis() % 100000000L);
        driver.findElement(By.name("telefono")).sendKeys("3109876543");
        driver.findElement(By.name("contrasena")).sendKeys("Docente123!");

        // Guardar
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Verificar que el docente aparece en la lista
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText();
            return body.contains("Pedro") || body.contains("creado") || body.contains("guardado");
        });

        String body = driver.findElement(By.tagName("body")).getText();
        assertTrue(body.contains("Pedro"),
                "El nuevo docente debe aparecer en la lista tras crearlo");
    }

    @Test
    @DisplayName("TC13 - Admin crea un nuevo estudiante")
    void crearEstudiante() {
        loginInstitucional(ADMIN_CORREO, ADMIN_PASS);

        clickTab("Estudiantes");

        WebElement botonNuevo = waitForClickable(
                By.xpath("//button[contains(.,'Nuevo') or contains(.,'Agregar') or contains(.,'Crear')]")
        );
        botonNuevo.click();

        waitFor(By.name("nombre")).sendKeys("Maria");
        driver.findElement(By.name("apellido")).sendKeys("Estudiante");

        String correoUnico = "estudiante.test." + System.currentTimeMillis() + "@unbosque.edu.co";
        driver.findElement(By.name("correo")).sendKeys(correoUnico);
        driver.findElement(By.name("cedula")).sendKeys("22" + System.currentTimeMillis() % 100000000L);
        driver.findElement(By.name("telefono")).sendKeys("3201234567");
        driver.findElement(By.name("contrasena")).sendKeys("Estudiante123!");

        // Semestre puede ser un select o input numérico
        try {
            driver.findElement(By.name("semestre")).sendKeys("5");
        } catch (Exception ignored) {
            // Campo opcional o con nombre distinto
        }

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText();
            return body.contains("Maria") || body.contains("creado") || body.contains("guardado");
        });

        String body = driver.findElement(By.tagName("body")).getText();
        assertTrue(body.contains("Maria"),
                "La nueva estudiante debe aparecer en la lista tras crearla");
    }

    @Test
    @DisplayName("TC14 - Admin visualiza la lista de sesiones")
    void verSesiones() {
        loginInstitucional(ADMIN_CORREO, ADMIN_PASS);

        clickTab("Sesiones");

        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("sesión") || body.contains("sesiones") || body.contains("no hay") || body.contains("fecha");
        });

        // Debe haber cargado la sección sin error de servidor
        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertFalse(body.contains("error 500") || body.contains("error de servidor"),
                "La lista de sesiones debe cargar sin errores de servidor");
    }
}
