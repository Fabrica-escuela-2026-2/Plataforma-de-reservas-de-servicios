package com.udea.service_platform.modules.categories.repository;

import com.udea.service_platform.modules.categories.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /**
     * Race-safe insert used when a service references a category that does not
     * exist yet. The unique constraint on "nombre" makes concurrent inserts
     * collapse into a single row.
     */
    @Modifying
    @Query(value = "INSERT INTO \"Categorias\" (nombre) VALUES (:nombre) "
            + "ON CONFLICT (nombre) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("nombre") String nombre);
}
