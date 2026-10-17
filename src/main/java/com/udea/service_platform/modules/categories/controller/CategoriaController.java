package com.udea.service_platform.modules.categories.controller;

import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;
import com.udea.service_platform.modules.categories.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    public ResponseEntity<CategoriaResponse> createCategory(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getAllCategories() {
        return ResponseEntity.ok(categoriaService.getAllCategories());
    }
}
