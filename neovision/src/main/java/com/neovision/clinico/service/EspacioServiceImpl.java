package com.neovision.clinico.service;

import com.neovision.clinico.dto.EspacioRequest;
import com.neovision.clinico.dto.EspacioResponse;
import com.neovision.clinico.dto.SalonRequest;
import com.neovision.clinico.dto.SalonResponse;
import com.neovision.clinico.entity.Espacio;
import com.neovision.clinico.entity.Salon;
import com.neovision.clinico.repository.EspacioRepository;
import com.neovision.clinico.repository.SalonRepository;
import com.neovision.common.config.CacheConfig;
import com.neovision.common.exception.EspacioNoEncontradoException;
import com.neovision.common.exception.SalonNoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;
    private final SalonRepository salonRepository;

    public EspacioServiceImpl(EspacioRepository espacioRepository, SalonRepository salonRepository) {
        this.espacioRepository = espacioRepository;
        this.salonRepository = salonRepository;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true)
    })
    public EspacioResponse crearEspacio(EspacioRequest request) {
        Espacio espacio = new Espacio();
        espacio.setNombre(request.getNombre());
        espacio.setDescripcion(request.getDescripcion());
        espacio.setActivo(true);
        espacioRepository.save(espacio);
        return toEspacioResponse(espacio);
    }

    @Override
    @Cacheable(CacheConfig.CACHE_ESPACIOS)
    @Transactional(readOnly = true)
    public List<EspacioResponse> listarEspacios() {
        return espacioRepository.findAll().stream()
                .map(this::toEspacioResponse)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true)
    })
    public EspacioResponse editarEspacio(Integer id, EspacioRequest request) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException("Espacio no encontrado"));
        espacio.setNombre(request.getNombre());
        espacio.setDescripcion(request.getDescripcion());
        espacioRepository.save(espacio);
        return toEspacioResponse(espacio);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    public void desactivarEspacio(Integer id) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException("Espacio no encontrado"));
        espacio.setActivo(false);
        espacioRepository.save(espacio);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true)
    })
    public SalonResponse crearSalon(SalonRequest request) {
        Espacio espacio = espacioRepository.findById(request.getIdEspacio())
                .orElseThrow(() -> new EspacioNoEncontradoException("Espacio no encontrado"));

        Salon salon = new Salon();
        salon.setNombre(request.getNombre());
        salon.setActivo(true);
        salon.setEspacio(espacio);
        salonRepository.save(salon);
        return toSalonResponse(salon);
    }

    // Evita tambien CACHE_ESPACIOS: listarEspacios() embebe la lista de salones
    // de cada espacio, asi que editar un salon sin invalidar ese cache dejaba la
    // vista de "Espacios" mostrando el nombre/estado viejo del salon hasta que
    // expirara el TTL de 10 minutos.
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    })
    public SalonResponse editarSalon(Integer id, SalonRequest request) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        salon.setNombre(request.getNombre());
        salonRepository.save(salon);
        return toSalonResponse(salon);
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_SALONES, key = "#idEspacio")
    @Transactional(readOnly = true)
    public List<SalonResponse> listarSalonesPorEspacio(Integer idEspacio) {
        return salonRepository.findByEspacioIdEspacio(idEspacio).stream()
                .map(this::toSalonResponse)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    })
    public void desactivarSalon(Integer id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        salon.setActivo(false);
        salonRepository.save(salon);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    public void activarEspacio(Integer id) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException("Espacio no encontrado"));
        espacio.setActivo(true);
        espacioRepository.save(espacio);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    })
    public void activarSalon(Integer id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        salon.setActivo(true);
        salonRepository.save(salon);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true)
    })
    public void eliminarEspacio(Integer id) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException("Espacio no encontrado"));
        try {
            espacioRepository.delete(espacio);
            espacioRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: el espacio tiene salones o sesiones asociadas. Desactívalo en su lugar.");
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_SALONES, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_ESPACIOS, allEntries = true)
    })
    public void eliminarSalon(Integer id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new SalonNoEncontradoException("Salon no encontrado"));
        try {
            salonRepository.delete(salon);
            salonRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: el salón tiene sesiones asociadas. Desactívalo en su lugar.");
        }
    }

    private EspacioResponse toEspacioResponse(Espacio espacio) {
        List<SalonResponse> salones = espacio.getSalones() != null
                ? espacio.getSalones().stream().map(this::toSalonResponse).toList()
                : List.of();
        return new EspacioResponse(
                espacio.getIdEspacio(),
                espacio.getNombre(),
                espacio.getDescripcion(),
                espacio.getActivo(),
                salones
        );
    }

    private SalonResponse toSalonResponse(Salon salon) {
        return new SalonResponse(
                salon.getIdSalon(),
                salon.getNombre(),
                salon.getActivo(),
                salon.getEspacio().getIdEspacio(),
                salon.getEspacio().getNombre()
        );
    }
}
