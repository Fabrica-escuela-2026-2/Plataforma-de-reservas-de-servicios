package com.udea.service_platform.modules.users.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InactivarCuentaRequest {

    @NotNull(message = "Debe confirmar la inactivación de la cuenta")
    @AssertTrue(message = "Debe confirmar la inactivación de la cuenta")
    private Boolean confirmar;
}
