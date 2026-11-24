package com.udea.service_platform.modules.resources.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"Recursos\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Long idRecursos;

    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;
}
