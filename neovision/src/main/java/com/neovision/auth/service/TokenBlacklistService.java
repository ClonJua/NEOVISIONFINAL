package com.neovision.auth.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> jtisRevocados = new ConcurrentHashMap<>();

    public void revocar(String jti, Instant expiraEn) {
        if (jti == null) return;
        jtisRevocados.put(jti, expiraEn);
    }

    public boolean estaRevocado(String jti) {
        if (jti == null) return false;
        Instant expiraEn = jtisRevocados.get(jti);
        if (expiraEn == null) return false;
        if (Instant.now().isAfter(expiraEn)) {
            // El token ya habria expirado por si solo; ya no hace falta recordarlo.
            jtisRevocados.remove(jti);
            return false;
        }
        return true;
    }
}
