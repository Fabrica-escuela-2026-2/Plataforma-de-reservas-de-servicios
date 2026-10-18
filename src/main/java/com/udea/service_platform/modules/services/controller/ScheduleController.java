package com.udea.service_platform.modules.services.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.services.dto.HorarioRequest;
import com.udea.service_platform.modules.services.dto.HorarioResponse;
import com.udea.service_platform.modules.services.service.HorarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final HorarioService horarioService;

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<List<HorarioResponse>> defineSchedule(
            @PathVariable Long serviceId,
            @RequestBody List<HorarioRequest> requests,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<HorarioResponse> responses = horarioService.defineSchedule(
                serviceId, requests, userDetails.getIdUsuario());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<List<HorarioResponse>> getSchedule(
            @PathVariable Long serviceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<HorarioResponse> responses = horarioService.getSchedule(
                serviceId, userDetails.getIdUsuario());
        return ResponseEntity.ok(responses);
    }
}
