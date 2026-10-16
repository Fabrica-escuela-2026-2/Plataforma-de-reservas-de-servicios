package com.udea.service_platform.modules.reservations.model;

import com.udea.service_platform.modules.services.model.Service;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "\"Reservas\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "id_estado", nullable = false)
    private Integer idEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", referencedColumnName = "id_servicio", nullable = false)
    private Service servicio;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalTime fechaInicio;

    @Column(name = "valor_acordado", precision = 38, scale = 2)
    private BigDecimal valorAcordado;

    @Column(name = "duracion_acordada")
    private Integer duracionAcordada;
}
