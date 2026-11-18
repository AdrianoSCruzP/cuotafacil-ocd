package com.cuotafacil.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros del simulador. En los ambientes llegan desde el config map {@code cuotafacil-config}
 * como variables de entorno (por ejemplo {@code CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES}); los
 * valores de {@code application.yml} solo sirven para correr en local.
 *
 * <p>Se leen al arrancar: un cambio en el config map no llega a los pods que ya están corriendo
 * hasta que se reinician.
 */
@Validated
@ConfigurationProperties("cuotafacil.simulador")
public record SimuladorProperties(
        @NotBlank String moneda,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal teaBase,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal teaPreferenteHaberes,
        @NotNull @Positive BigDecimal montoMinimo,
        @NotNull @Positive BigDecimal montoMaximo,
        @Positive int plazoMinimoMeses,
        @Positive int plazoMaximoMeses) {

    @AssertTrue(message = "la TEA preferente para clientes con haberes debe ser menor que la TEA base")
    public boolean isTasaPreferenteMenorQueBase() {
        return teaBase == null || teaPreferenteHaberes == null || teaPreferenteHaberes.compareTo(teaBase) < 0;
    }

    @AssertTrue(message = "el monto mínimo debe ser menor que el monto máximo")
    public boolean isRangoMontoValido() {
        return montoMinimo == null || montoMaximo == null || montoMinimo.compareTo(montoMaximo) < 0;
    }

    @AssertTrue(message = "el plazo mínimo no puede superar al plazo máximo")
    public boolean isRangoPlazoValido() {
        return plazoMinimoMeses <= plazoMaximoMeses;
    }
}
