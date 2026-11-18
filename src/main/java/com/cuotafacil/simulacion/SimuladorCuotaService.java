package com.cuotafacil.simulacion;

import com.cuotafacil.config.SimuladorProperties;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Cuota fija mensual (sistema francés): {@code cuota = P * i * (1+i)^n / ((1+i)^n - 1)},
 * con {@code i} la tasa efectiva mensual equivalente a la TEA: {@code (1 + TEA)^(1/12) - 1}.
 */
@Service
public class SimuladorCuotaService {

    private static final MathContext PRECISION = MathContext.DECIMAL64;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final SimuladorProperties propiedades;

    public SimuladorCuotaService(SimuladorProperties propiedades) {
        this.propiedades = propiedades;
    }

    public SimulacionResponse simular(BigDecimal monto, int plazoMeses, boolean conHaberes) {
        validarMonto(monto);
        validarPlazo(plazoMeses);

        TipoTasa tipoTasa = conHaberes ? TipoTasa.PREFERENTE_HABERES : TipoTasa.BASE;
        BigDecimal tea = conHaberes ? propiedades.teaPreferenteHaberes() : propiedades.teaBase();
        BigDecimal tem = temEquivalente(tea);

        BigDecimal factor = BigDecimal.ONE.add(tem).pow(plazoMeses, PRECISION);
        BigDecimal cuota = monto.multiply(tem, PRECISION)
                .multiply(factor, PRECISION)
                .divide(factor.subtract(BigDecimal.ONE), PRECISION)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAPagar = cuota.multiply(BigDecimal.valueOf(plazoMeses));

        return new SimulacionResponse(
                propiedades.moneda(),
                monto.setScale(2, RoundingMode.HALF_UP),
                plazoMeses,
                tipoTasa,
                porcentaje(tea, 2),
                porcentaje(tem, 4),
                cuota,
                totalAPagar,
                totalAPagar.subtract(monto).setScale(2, RoundingMode.HALF_UP));
    }

    public CondicionesResponse condiciones() {
        return new CondicionesResponse(
                propiedades.moneda(),
                porcentaje(propiedades.teaBase(), 2),
                porcentaje(propiedades.teaPreferenteHaberes(), 2),
                propiedades.montoMinimo(),
                propiedades.montoMaximo(),
                propiedades.plazoMinimoMeses(),
                propiedades.plazoMaximoMeses());
    }

    private void validarMonto(BigDecimal monto) {
        if (monto.compareTo(propiedades.montoMinimo()) < 0 || monto.compareTo(propiedades.montoMaximo()) > 0) {
            throw new SimulacionInvalidaException("El monto debe estar entre %s y %s %s".formatted(
                    propiedades.montoMinimo().toPlainString(),
                    propiedades.montoMaximo().toPlainString(),
                    propiedades.moneda()));
        }
    }

    private void validarPlazo(int plazoMeses) {
        if (plazoMeses < propiedades.plazoMinimoMeses() || plazoMeses > propiedades.plazoMaximoMeses()) {
            throw new SimulacionInvalidaException("El plazo debe estar entre %d y %d meses".formatted(
                    propiedades.plazoMinimoMeses(), propiedades.plazoMaximoMeses()));
        }
    }

    private static BigDecimal temEquivalente(BigDecimal tea) {
        return BigDecimal.valueOf(Math.pow(1 + tea.doubleValue(), 1.0 / 12) - 1);
    }

    private static BigDecimal porcentaje(BigDecimal tasa, int decimales) {
        return tasa.multiply(CIEN).setScale(decimales, RoundingMode.HALF_UP);
    }
}
