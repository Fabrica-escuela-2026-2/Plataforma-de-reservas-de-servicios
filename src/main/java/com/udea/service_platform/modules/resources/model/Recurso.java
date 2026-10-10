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
    @Column(name = "id_recursos")
    private Long idRecursos;
}