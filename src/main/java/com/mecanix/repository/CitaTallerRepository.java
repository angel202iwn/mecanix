package com.mecanix.repository;

import com.mecanix.model.CitaTaller;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaTallerRepository extends JpaRepository<CitaTaller, Integer> {
    List<CitaTaller> findAllByOrderByFechaHoraCitaDesc();
}