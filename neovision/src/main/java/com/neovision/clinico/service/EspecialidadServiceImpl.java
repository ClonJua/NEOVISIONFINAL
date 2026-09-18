package com.neovision.clinico.service;

import com.neovision.clinico.dto.EspecialidadRequest;
import com.neovision.clinico.dto.EspecialidadResponse;
import com.neovision.clinico.entity.Especialidad;
import com.neovision.clinico.repository.EspecialidadRepository;
import com.neovision.common.exception.EspecialidadNoEncontradaException;
import com.neovision.common.exception.RecursoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadServiceImpl(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarTodas() {
        return especialidadRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarActivas() {
        return especialidadRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspecialidadResponse obtener(Integer id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
        return toResponse(e);
    }

    @Override
    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest req) {
        if (especialidadRepository.existsByNombreIgnoreCase(req.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una especialidad con ese nombre");
        }
        Especialidad e = new Especialidad();
        e.setNombre(req.getNombre().trim());
        e.setObservacionRecordatorio(req.getObservacionRecordatorio());
        e.setRequiereSubtipo(Boolean.TRUE.equals(req.getRequiereSubtipo()));
        e.setEsDefault(Boolean.TRUE.equals(req.getEsDefault()));
        e.setActivo(true);
        return toResponse(especialidadRepository.save(e));
    }

    @Override
    @Transactional
    public EspecialidadResponse actualizar(Integer id, EspecialidadRequest req) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
        if (!e.getNombre().equalsIgnoreCase(req.getNombre())
                && especialidadRepository.existsByNombreIgnoreCase(req.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una especialidad con ese nombre");
        }
        e.setNombre(req.getNombre().trim());
        e.setObservacionRecordatorio(req.getObservacionRecordatorio());
        if (req.getRequiereSubtipo() != null) e.setRequiereSubtipo(req.getRequiereSubtipo());
        if (req.getEsDefault() != null) e.setEsDefault(req.getEsDefault());
        return toResponse(especialidadRepository.save(e));
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
        e.setActivo(false);
        especialidadRepository.save(e);
    }

    @Override
    @Transactional
    public void reactivar(Integer id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException("Especialidad no encontrada"));
        e.setActivo(true);
        especialidadRepository.save(e);
    }

    private EspecialidadResponse toResponse(Especialidad e) {
        return new EspecialidadResponse(
                e.getIdEspecialidad(),
                e.getNombre(),
                e.getObservacionRecordatorio(),
                e.getRequiereSubtipo(),
                e.getEsDefault(),
                e.getActivo()
        );
    }
}
