package com.udea.service_platform.modules.reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Page of "Mis reservas" (HU-16). It mirrors the fields of Spring's Page
 * ({@code content}, {@code number}, {@code size}, {@code totalElements},
 * {@code totalPages}) and adds {@code mensaje}, which is filled only when
 * the user has no reservations at all.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MisReservasResponse {

    public static final String MENSAJE_SIN_RESERVAS = "Todavía no tienes reservas";

    private String mensaje;
    private List<MiReservaResponse> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
}
