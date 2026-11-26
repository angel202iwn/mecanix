package com.mecanix.repository;

import com.mecanix.model.DetalleOrden;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Integer> {
    List<DetalleOrden> findByOrdenTrabajo_IdOrden(Integer idOrden);
}