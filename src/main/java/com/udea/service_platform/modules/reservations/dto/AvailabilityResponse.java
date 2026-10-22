package com.udea.service_platform.modules.reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    private LocalDate fecha;
    private Long idServicio;
    private Integer duracionMinutos;
    private List<AvailabilitySlotResponse> slots;
}
