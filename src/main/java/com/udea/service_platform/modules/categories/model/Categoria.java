package com.udea.service_platform.modules.categories.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"Categorias\"", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categorias_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;
}
