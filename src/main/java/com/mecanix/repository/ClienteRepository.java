// ClienteRepository.java
package com.mecanix.repository;
import com.mecanix.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByDniRuc(String dniRuc);
}