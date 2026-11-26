// VehiculoRepository.java
package com.mecanix.repository;
import com.mecanix.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {
    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);
}