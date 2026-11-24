package com.udea.service_platform.modules.resources.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.resources.dto.RecursoRequest;
import com.udea.service_platform.modules.resources.dto.RecursoResponse;
import com.udea.service_platform.modules.resources.service.RecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;

    @Operation(summary = "Registrar un recurso fisico",
            description = "El proveedor se toma del token; el administrador debe indicar idProveedor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso registrado"),
            @ApiResponse(responseCode = "400", description = "Nombre obligatorio o proveedor no indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un recurso con ese nombre")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    public ResponseEntity<RecursoResponse> crearRecurso(
            @Valid @RequestBody RecursoRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RecursoResponse response = recursoService.crear(request, userDetails.getIdUsuario(), esAdmin(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Mis recursos", description = "Recursos activos e inactivos con sus servicios vinculados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de recursos del proveedor")
    })
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    public ResponseEntity<List<RecursoResponse>> misRecursos(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(recursoService.misRecursos(userDetails.getIdUsuario()));
    }

    @Operation(summary = "Listar recursos", description = "Consulta de administrador, opcionalmente por proveedor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de recursos")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RecursoResponse>> listarRecursos(
            @Parameter(description = "ID del proveedor", example = "9701")
            @RequestParam(required = false) Long idProveedor) {
        return ResponseEntity.ok(recursoService.listar(idProveedor));
    }

    @Operation(summary = "Dar de baja un recurso",
            description = "Bloquea la baja si existen reservas futuras que ocupan agenda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurso inactivo o ya estaba inactivo"),
            @ApiResponse(responseCode = "403", description = "El recurso no pertenece al proveedor"),
            @ApiResponse(responseCode = "404", description = "El recurso no existe"),
            @ApiResponse(responseCode = "409", description = "El recurso tiene reservas pendientes")
    })
    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    public ResponseEntity<RecursoResponse> inactivarRecurso(
            @Parameter(description = "ID del recurso", example = "9701") @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RecursoResponse response = recursoService.inactivar(id, userDetails.getIdUsuario(), esAdmin(userDetails));
        return ResponseEntity.ok(response);
    }

    private boolean esAdmin(UserDetailsImpl userDetails) {
        return "ADMIN".equals(userDetails.getRol());
    }
}
