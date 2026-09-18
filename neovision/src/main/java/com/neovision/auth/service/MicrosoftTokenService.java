package com.neovision.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
@Schema(description = "Genera logica de inicio de sesion con Microsoft, valida el token con la API de Microsoft Graph y obtiene el correo del usuario.")

public class MicrosoftTokenService {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftTokenService.class);
    private static final String GRAPH_ME_URL = "https://graph.microsoft.com/v1.0/me";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${microsoft.expected-client-id:c441594b-ed0a-490c-abee-138cb21acef1}")
    private String expectedClientId;

    public String extractEmail(String accessToken) {
        validarAudiencia(accessToken);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_ME_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Token de Microsoft inválido");
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode mailNode = json.get("mail");
            JsonNode upnNode = json.get("userPrincipalName");
            String email;
            if (mailNode != null && !mailNode.isNull()) {
                email = mailNode.asText();
            } else if (upnNode != null && !upnNode.isNull()) {
                email = upnNode.asText();
            } else {
                throw new RuntimeException("La cuenta de Microsoft no tiene un correo asociado");
            }

            return email.toLowerCase();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al validar token de Microsoft", e);
        }
    }

    /**
     * Verifica, si es posible, que el access token haya sido emitido para la
     * app de NeoVision (claim "appid" o "azp") y no para otra aplicacion que
     * tambien tenga scope User.Read sobre Graph.
     */
    private void validarAudiencia(String accessToken) {
        try {
            String[] partes = accessToken.split("\\.");
            if (partes.length != 3) return; // no es un JWT decodificable: no se puede validar, se deja pasar
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(payloadJson);
            String appid = payload.has("appid") ? payload.get("appid").asText()
                    : payload.has("azp") ? payload.get("azp").asText() : null;
            if (appid != null && !appid.equalsIgnoreCase(expectedClientId)) {
                throw new RuntimeException("El token de Microsoft no fue emitido para esta aplicación");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.debug("No se pudo decodificar el access token de Microsoft para validar audiencia (se continua sin este chequeo): {}", e.getMessage());
        }
    }
}
