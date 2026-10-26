package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String fecha) {
        if (serviceId == null) {
            throw new IllegalArgumentException("El ID del servicio es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        LocalDate fechaParseada;
        try {
            fechaParseada = LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)");
        }

        AvailabilityResponse response = availabilityService.getAvailability(serviceId, fechaParseada);
        return ResponseEntity.ok(response);
    }
}
