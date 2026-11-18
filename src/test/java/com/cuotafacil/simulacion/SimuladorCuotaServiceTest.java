package com.cuotafacil.simulacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cuotafacil.config.SimuladorProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Valores esperados calculados aparte con la misma fórmula (Python, Decimal de 40 dígitos). */
class SimuladorCuotaServiceTest {

    private static SimuladorProperties propiedades(int plazoMaximoMeses) {
        return new SimuladorProperties("PEN", new BigDecimal("0.2490"), new BigDecimal("0.1990"),
                new BigDecimal("1000"), new BigDecimal("150000"), 6, plazoMaximoMeses);
    }

    private final SimuladorCuotaService simulador = new SimuladorCuotaService(propiedades(60));

    @Test
    void sinHaberesAplicaLaTasaBase() {
        SimulacionResponse r = simulador.simular(new BigDecimal("10000"), 12, false);

        assertThat(r.tipoTasa()).isEqualTo(TipoTasa.BASE);
        assertThat(r.teaPorcentaje()).isEqualByComparingTo("24.90");
        assertThat(r.temPorcentaje()).isEqualByComparingTo("1.8701");
        assertThat(r.cuotaMensual()).isEqualByComparingTo("938.07");
        assertThat(r.totalAPagar()).isEqualByComparingTo("11256.84");
        assertThat(r.totalIntereses()).isEqualByComparingTo("1256.84");
    }

    @Test
    void conHaberesAplicaLaTasaPreferente() {
        SimulacionResponse r = simulador.simular(new BigDecimal("10000"), 12, true);

        assertThat(r.tipoTasa()).isEqualTo(TipoTasa.PREFERENTE_HABERES);
        assertThat(r.teaPorcentaje()).isEqualByComparingTo("19.90");
        assertThat(r.temPorcentaje()).isEqualByComparingTo("1.5239");
        assertThat(r.cuotaMensual()).isEqualByComparingTo("918.17");
        assertThat(r.totalIntereses()).isEqualByComparingTo("1018.04");
    }

    @Test
    void laTasaPreferenteDaUnaCuotaMenorQueLaBase() {
        BigDecimal base = simulador.simular(new BigDecimal("20000"), 60, false).cuotaMensual();
        BigDecimal preferente = simulador.simular(new BigDecimal("20000"), 60, true).cuotaMensual();

        assertThat(base).isEqualByComparingTo("557.41");
        assertThat(preferente).isLessThan(base);
    }

    @Test
    void rechazaUnPlazoMayorAlMaximoConfigurado() {
        assertThatThrownBy(() -> simulador.simular(new BigDecimal("20000"), 72, true))
                .isInstanceOf(SimulacionInvalidaException.class)
                .hasMessage("El plazo debe estar entre 6 y 60 meses");
    }

    @Test
    void rechazaUnPlazoMenorAlMinimo() {
        assertThatThrownBy(() -> simulador.simular(new BigDecimal("20000"), 5, false))
                .isInstanceOf(SimulacionInvalidaException.class);
    }

    @Test
    void rechazaMontosFueraDelRango() {
        assertThatThrownBy(() -> simulador.simular(new BigDecimal("999.99"), 12, false))
                .isInstanceOf(SimulacionInvalidaException.class)
                .hasMessage("El monto debe estar entre 1000 y 150000 PEN");
        assertThatThrownBy(() -> simulador.simular(new BigDecimal("150000.01"), 12, false))
                .isInstanceOf(SimulacionInvalidaException.class);
    }

    @Test
    void cf241ConPlazoMaximo72AceptaLos72Meses() {
        SimuladorCuotaService con72 = new SimuladorCuotaService(propiedades(72));

        SimulacionResponse r = con72.simular(new BigDecimal("20000"), 72, true);

        assertThat(r.plazoMeses()).isEqualTo(72);
        assertThat(r.cuotaMensual()).isEqualByComparingTo("459.40");
        assertThat(r.totalAPagar()).isEqualByComparingTo("33076.80");
    }
}
