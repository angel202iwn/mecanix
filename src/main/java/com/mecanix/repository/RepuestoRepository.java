// RepuestoRepository.java
package com.mecanix.repository;
import com.mecanix.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepuestoRepository extends JpaRepository<Repuesto, Integer> {}