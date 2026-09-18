package com.neovision.clinico.historia.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Utilidad para el sello de integridad SHA-256 de las atenciones finalizadas. */
public final class HashUtil {

    private HashUtil() {}

    public static String sha256Hex(String contenido) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular el sello de integridad", e);
        }
    }
}
