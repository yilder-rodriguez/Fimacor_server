package com.fimacor.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.usuarios.entity.DocumentoIdentidad;

public interface DocumentoIdentidadRepository extends JpaRepository<DocumentoIdentidad, Integer> {
}
