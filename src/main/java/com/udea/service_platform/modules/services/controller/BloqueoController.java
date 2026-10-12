package com.udea.service_platform.modules.services.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;
import com.udea.service_platform.modules.services.service.BloqueoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @PostMapping("/provider")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<BloqueoResponse> createBlock(
            @Valid @RequestBody BloqueoRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        BloqueoResponse response = bloqueoService.createBlock(request, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<List<BloqueoResponse>> getActiveBlocks(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<BloqueoResponse> responses = bloqueoService.getActiveBlocks(userDetails.getIdUsuario());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<Void> deleteBlock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        bloqueoService.deleteBlock(id, userDetails.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}
