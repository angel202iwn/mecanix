package com.mecanix.repository;

import com.mecanix.model.MovimientoInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {
    List<MovimientoInventario> findAllByOrderByIdMovimientoDesc();
    List<MovimientoInventario> findByRepuesto_IdRepuestoOrderByIdMovimientoDesc(Integer idRepuesto);
}