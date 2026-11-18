package com.cuotafacil.simulacion;

import java.math.BigDecimal;

public record SimulacionResponse(
        String moneda,
        BigDecimal monto,
        int plazoMeses,
        TipoTasa tipoTasa,
        BigDecimal teaPorcentaje,
        BigDecimal temPorcentaje,
        BigDecimal cuotaMensual,
        BigDecimal totalAPagar,
        BigDecimal totalIntereses) {
}
