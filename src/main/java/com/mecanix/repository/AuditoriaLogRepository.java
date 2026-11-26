package com.mecanix.repository;

import com.mecanix.model.AuditoriaLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Integer> {
    List<AuditoriaLog> findTop30ByOrderByIdLogDesc();
    List<AuditoriaLog> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);
    List<AuditoriaLog> findByModuloAndFechaHoraBetweenOrderByFechaHoraDesc(String modulo, LocalDateTime inicio, LocalDateTime fin);
}