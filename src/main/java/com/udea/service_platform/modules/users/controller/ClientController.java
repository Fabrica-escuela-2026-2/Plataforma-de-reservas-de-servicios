package com.udea.service_platform.modules.users.controller;

import com.udea.service_platform.modules.reservations.service.ReservaService;
import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.dto.ClientSummaryResponse;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import com.udea.service_platform.modules.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClientController {

    private final UserService userService;
    private final ReservaService reservaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROVEEDOR', 'ADMIN')")
    @Operation(summary = "Buscar clientes")
    public ResponseEntity<Page<ClientSummaryResponse>> searchClients(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ClientSummaryResponse> results = userService.searchClients(searchTerm, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVEEDOR', 'ADMIN')")
    @Operation(summary = "Consultar el detalle de un cliente")
    public ResponseEntity<ClientDetailResponse> getClientDetail(@PathVariable Long id) {
        ClientDetailResponse detail = userService.getClientDetail(id);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasAnyRole('PROVEEDOR', 'ADMIN')")
    @Operation(summary = "Consultar las reservas de un cliente")
    public ResponseEntity<Page<ReservaSummaryResponse>> getClientReservations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReservaSummaryResponse> reservations = reservaService.getClientReservations(id, PageRequest.of(page, size));
        return ResponseEntity.ok(reservations);
    }
}
