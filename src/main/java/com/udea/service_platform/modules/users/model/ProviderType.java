package com.udea.service_platform.modules.users.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"Tipo_Proveedores\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderType {

    @Id
    @Column(name = "id_tipo_proveedor")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;
}
