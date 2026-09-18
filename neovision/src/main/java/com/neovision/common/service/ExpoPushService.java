package com.neovision.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envia notificaciones push a traves de la API de Expo.
 * Expo se encarga de enrutar a APNs (iOS) y FCM (Android).
 * Reutiliza el patron de {@code MicrosoftTokenService} (HttpClient nativo, sin deps extra).
 */
@Service
public class ExpoPushService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envia una notificacion push. No lanza excepcion: registra el error y continua,
     * para que un fallo de push nunca rompa el flujo que lo invoca (ej. el scheduler).
     *
     * @param expoPushToken token del dispositivo destino (puede ser null/blank → se ignora)
     * @param titulo        titulo de la notificacion
     * @param cuerpo        cuerpo de la notificacion
     */
    public void enviar(String expoPushToken, String titulo, String cuerpo) {
        enviar(expoPushToken, titulo, cuerpo, null);
    }

    /**
     * Igual que {@link #enviar(String, String, String)} pero adjunta el id de la
     * cita en {@code data}, para que la app pueda abrir la cita al tocar la push.
     */
    public void enviar(String expoPushToken, String titulo, String cuerpo, Integer idCita) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("to", expoPushToken);
            payload.put("title", titulo);
            payload.put("body", cuerpo);
            payload.put("sound", "default");
            if (idCita != null) {
                ObjectNode data = payload.putObject("data");
                data.put("idCita", idCita);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXPO_PUSH_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("PUSH: Expo respondio {} para token {}: {}",
                        response.statusCode(), expoPushToken, response.body());
            } else {
                log.info("PUSH: Notificacion enviada a {}", expoPushToken);
            }
        } catch (Exception e) {
            log.error("PUSH: Error enviando notificacion a {}: {}", expoPushToken, e.getMessage());
        }
    }
}
