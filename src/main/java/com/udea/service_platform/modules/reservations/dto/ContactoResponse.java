package com.udea.service_platform.modules.reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contact data of the other party of a reservation: the provider when the
 * caller is a client, the client when the caller is a provider.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactoResponse {

    /** CLIENTE or PROVEEDOR: who the contact is. */
    private String rol;
    private String nombre;
    private String correo;
    private String telefono;
}
