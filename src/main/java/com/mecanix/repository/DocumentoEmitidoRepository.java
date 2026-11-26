// DocumentoEmitidoRepository.java
package com.mecanix.repository;
import com.mecanix.model.DocumentoEmitido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoEmitidoRepository extends JpaRepository<DocumentoEmitido, Integer> {
    List<DocumentoEmitido> findAllByOrderByIdDocumentoDesc();
}