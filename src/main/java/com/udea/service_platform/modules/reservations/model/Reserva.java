package com.udea.service_platform.modules.reservations.model;

import com.udea.service_platform.modules.services.model.Service;
import jakarta.persistence.*;
import lombok.*;

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
    @Column(name = "id_reserva")
    private Long id;

    @Column(name = "estado_reserva", nullable = false)
    private Integer idEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_reserva", referencedColumnName = "id_servicio", nullable = false)
    private Service servicio;

    @Column(name = "usuario_reserva", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalTime fechaInicio;
}
