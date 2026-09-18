package com.neovision.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC01–TC04: Flujos de autenticación y registro.
 */
@DisplayName("Auth: Login y Registro")
class AuthTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC01 - Login exitoso como paciente")
    void loginExitosoPaciente() {
        loginPaciente(PACIENTE_CORREO, PACIENTE_PASS);

        // Después del login el paciente llega a su dashboard
        wait.until(driver -> !driver.getCurrentUrl().contains("/login"));
        assertFalse(driver.getCurrentUrl().contains("/login"),
                "Debe redirigir fuera de /login tras autenticarse");

        // Debe existir el botón de logout en la página
        assertTrue(isVisible(By.cssSelector("[aria-label='Cerrar sesion']")),
                "El botón de cerrar sesión debe ser visible en el dashboard");
    }

    @Test
    @DisplayName("TC02 - Login con credenciales incorrectas muestra error")
    void loginCredencialesIncorrectas() {
        driver.get(BASE_URL + "/login");
        waitForClickable(By.xpath("//button[@role='tab'][contains(.,'Soy Paciente')]")).click();
        waitFor(By.name("correo")).sendKeys("noexiste@test.com");
        driver.findElement(By.name("contrasena")).sendKeys("clavemal");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Debe aparecer algún mensaje de error en la página
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("incorrecta") || body.contains("inválid") || body.contains("incorrecto")
                    || body.contains("no encontrado") || body.contains("error");
        });

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Debe permanecer en /login con credenciales incorrectas");
    }

    @Test
    @DisplayName("TC03 - Registro de nuevo paciente con todos los campos")
    void registroNuevoPaciente() {
        driver.get(BASE_URL + "/registro");

        waitFor(By.name("nombre")).sendKeys("Juan");
        driver.findElement(By.name("apellido")).sendKeys("Prueba");

        String correoUnico = "paciente.test." + System.currentTimeMillis() + "@correo.com";
        driver.findElement(By.name("correo")).sendKeys(correoUnico);
        driver.findElement(By.name("contrasena")).sendKeys("Prueba123!");

        new Select(driver.findElement(By.name("tipoDocumento"))).selectByValue("CC");
        driver.findElement(By.name("numeroDocumento")).sendKeys("1234567890");
        driver.findElement(By.name("fechaNacimiento")).sendKeys("2000-01-15");
        driver.findElement(By.name("telefono")).sendKeys("3001234567");

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Tras el registro exitoso debe redirigir o mostrar confirmación
        wait.until(driver -> {
            String url = driver.getCurrentUrl();
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return !url.contains("/registro") || body.contains("éxito") || body.contains("bienvenid");
        });

        // No debe mostrar error de validación
        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertFalse(body.contains("ya registrado") && body.contains("error"),
                "No debe mostrar error de correo duplicado con un correo nuevo");
    }

    @Test
    @DisplayName("TC04 - Registro sin llenar campos obligatorios muestra validación")
    void registroCamposVacios() {
        driver.get(BASE_URL + "/registro");

        // Submit sin ingresar nada
        waitFor(By.cssSelector("button[type='submit']")).click();

        // El formulario no debe enviar (permanece en /registro)
        // y el navegador activa la validación HTML5 (campo requerido)
        String url = driver.getCurrentUrl();
        assertTrue(url.contains("/registro"),
                "Debe permanecer en /registro si hay campos requeridos vacíos");

        // El campo nombre debe tener validación nativa o algún error visible
        String nombre = driver.findElement(By.name("nombre")).getAttribute("required");
        assertNotNull(nombre, "El campo nombre debe tener el atributo required");
    }
}
