package com.cuotafacil.simulacion;

import java.math.BigDecimal;

/** Condiciones vigentes del simulador, tal como las cargó el config map del ambiente. */
public record CondicionesResponse(
        String moneda,
        BigDecimal teaBasePorcentaje,
        BigDecimal teaPreferenteHaberesPorcentaje,
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        int plazoMinimoMeses,
        int plazoMaximoMeses) {
}
