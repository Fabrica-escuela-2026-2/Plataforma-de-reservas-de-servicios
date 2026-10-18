package com.udea.service_platform.modules.users.repository;

import com.udea.service_platform.modules.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByCorreo(String correo);

    Optional<User> findByCorreo(String correo);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM \"Ciudades\" WHERE id_ciudad = :id)", nativeQuery = true)
    boolean existsCiudad(@Param("id") Long id);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM \"Tipo_documento\" WHERE id_tipo_documento = :id)", nativeQuery = true)
    boolean existsTipoDocumento(@Param("id") Long id);
}
