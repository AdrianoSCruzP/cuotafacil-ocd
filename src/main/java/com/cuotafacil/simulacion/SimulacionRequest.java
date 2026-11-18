package com.cuotafacil.simulacion;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SimulacionRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor que cero")
        @Digits(integer = 9, fraction = 2, message = "admite hasta 2 decimales")
        BigDecimal monto,

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor que cero")
        Integer plazoMeses,

        @NotNull(message = "es obligatorio")
        Boolean conHaberes) {
}
