package com.udea.service_platform.modules.users.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.users.dto.*;
import com.udea.service_platform.modules.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/provider-onboarding")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<UserProfileResponse> onboardProvider(
            @Valid @RequestBody ProviderOnboardingRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserProfileResponse response = userService.onboardProvider(userDetails.getIdUsuario(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserProfileResponse response = userService.getProfile(userDetails.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserProfileResponse response = userService.updateProfile(userDetails.getIdUsuario(), request);
        return ResponseEntity.ok(response);
    }
}
