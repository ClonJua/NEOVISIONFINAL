package com.neovision.clinico.historia.controller;

import com.neovision.clinico.historia.dto.CupsResponse;
import com.neovision.clinico.historia.entity.Cups;
import com.neovision.clinico.historia.repository.CupsRepository;
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
 * Catálogo CUPS para autocompletar procedimientos. Bajo /api/historias/** hereda
 * el RBAC (estudiante/docente/admin). Solo lectura.
 */
@Tag(name = "Historia Clínica - CUPS", description = "Catálogo controlado de procedimientos (Clasificación Única de Procedimientos en Salud).")
@RestController
@RequestMapping("/api/historias/cups")
public class CupsController {

    private final CupsRepository cupsRepository;

    public CupsController(CupsRepository cupsRepository) {
        this.cupsRepository = cupsRepository;
    }

    @Operation(summary = "Buscar CUPS", description = "Autocompletar por código o descripción (máx. 20 resultados).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CupsResponse>>> buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        List<CupsResponse> resultados = cupsRepository.buscar(q.trim(), PageRequest.of(0, 20)).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("CUPS", resultados));
    }

    private CupsResponse toResponse(Cups c) {
        return new CupsResponse(c.getCodigo(), c.getDescripcion(), c.getCategoria());
    }
}
