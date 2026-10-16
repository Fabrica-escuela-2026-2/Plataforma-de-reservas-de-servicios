package com.udea.service_platform.modules.services.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"Dias_Semana\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaSemana {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;
}
