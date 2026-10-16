package com.udea.service_platform.modules.services.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "\"Horarios\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer idHorario;

    @Column(name = "dia_semana", nullable = false)
    private Integer idDiaSemana;

    @Column(name = "id_dia_semana", nullable = false)
    private Integer idDiaSemanaLegacy;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "servicio_horarios", nullable = false)
    private Integer idServicio;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicioLegacy;

    @PrePersist
    void sincronizarColumnasLegado() {
        idDiaSemanaLegacy = idDiaSemana;
        idServicioLegacy = idServicio;
    }
}
