// OrdenTrabajoRepository.java
package com.mecanix.repository;
import com.mecanix.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Integer> {
    List<OrdenTrabajo> findByEstado(String estado);
    List<OrdenTrabajo> findAllByOrderByIdOrdenDesc();
}