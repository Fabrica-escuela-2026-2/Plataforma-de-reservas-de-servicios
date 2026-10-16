package com.udea.service_platform.modules.services.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "\"Servicios\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "duracion", nullable = false)
    private Integer duracion;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "usuario_proveedor", nullable = false)
    private Long idProveedor;

    @Column(name = "id_recursos", nullable = false)
    private Long idRecursos;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
