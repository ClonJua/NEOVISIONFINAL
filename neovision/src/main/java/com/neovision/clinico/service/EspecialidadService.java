package com.neovision.clinico.service;

import com.neovision.clinico.dto.EspecialidadRequest;
import com.neovision.clinico.dto.EspecialidadResponse;

import java.util.List;

public interface EspecialidadService {

    List<EspecialidadResponse> listarTodas();

    List<EspecialidadResponse> listarActivas();

    EspecialidadResponse obtener(Integer id);

    EspecialidadResponse crear(EspecialidadRequest request);

    EspecialidadResponse actualizar(Integer id, EspecialidadRequest request);

    void desactivar(Integer id);

    void reactivar(Integer id);
}
