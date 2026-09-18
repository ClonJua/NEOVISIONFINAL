package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.Cie10Response;
import com.neovision.clinico.historia.entity.Cie10;
import com.neovision.clinico.historia.repository.Cie10Repository;
import com.neovision.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo CIE-10 para autocompletar diagnósticos. Bajo /api/historias/** hereda
 * el RBAC (estudiante/docente/admin). Solo lectura.
 */
@Tag(name = "Historia Clínica - CIE-10", description = "Catálogo controlado de diagnósticos.")
@RestController
@RequestMapping("/api/historias/cie10")
public class Cie10Controller {

    private final Cie10Repository cie10Repository;

    public Cie10Controller(Cie10Repository cie10Repository) {
        this.cie10Repository = cie10Repository;
    }

    @Operation(summary = "Buscar CIE-10", description = "Autocompletar por código o descripción (máx. 20 resultados).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Cie10Response>>> buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        List<Cie10Response> resultados = cie10Repository.buscar(q.trim(), PageRequest.of(0, 20)).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("CIE-10", resultados));
    }

    private Cie10Response toResponse(Cie10 c) {
        return new Cie10Response(c.getCodigo(), c.getDescripcion(), c.getCategoria());
    }
}
