package com.udea.service_platform.modules.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloqueoRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private LocalTime horaInicio;

    private LocalTime horaFin;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String motivo;
}
