package com.neovision.clinico.scheduler;

import com.neovision.clinico.repository.CitaRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class CitaScheduler {

    private static final Logger log = LoggerFactory.getLogger(CitaScheduler.class);

    private final CitaRepository citaRepository;

    public CitaScheduler(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }
    
    @Scheduled(fixedDelay = 300_000L)
    @SchedulerLock(name = "vencerCitasPasadas", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    @Transactional
    public void vencerCitasPasadas() {
        try {
            int afectadas = citaRepository.marcarVencidas(LocalDate.now(), LocalTime.now());
            if (afectadas > 0) {
                log.info("Vencidas {} citas pasadas", afectadas);
            }
        } catch (Exception e) {
            log.error("Error al marcar citas vencidas", e);
        }
    }
}
