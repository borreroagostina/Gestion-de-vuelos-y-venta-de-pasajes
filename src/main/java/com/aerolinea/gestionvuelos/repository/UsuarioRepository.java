package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  boolean existsByDocumento(String documento);

  boolean existsByEmailIgnoreCase(String email);

  Optional<Usuario> findByEmailIgnoreCase(String email);

  Optional<Usuario> findByTokenActivacion(String tokenActivacion);
}
