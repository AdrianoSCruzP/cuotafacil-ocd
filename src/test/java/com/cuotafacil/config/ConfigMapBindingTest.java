package com.cuotafacil.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.yaml.snakeyaml.Yaml;

/**
 * Una clave mal escrita en el config map no falla: Spring la ignora y el pod arranca con el valor
 * de application.yml. Aquí se carga cada config map como si fueran las variables de entorno del pod
 * y se comprueba que cada clave llega a su propiedad y que no sobra ni falta ninguna.
 */
class ConfigMapBindingTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"deploy/cert/configmap.yaml", "deploy/prod/configmap.yaml"})
    void cadaClaveDelConfigMapLlegaASuPropiedad(String rutaConfigMap) throws IOException {
        Map<String, Object> data = leerData(rutaConfigMap);

        SimuladorProperties propiedades = enlazar(data);

        Map<String, String> esperadas = new LinkedHashMap<>();
        Map<String, String> enlazadas = new LinkedHashMap<>();
        for (RecordComponent componente : SimuladorProperties.class.getRecordComponents()) {
            String variable = "CUOTAFACIL_SIMULADOR_" + componente.getName().toUpperCase();
            esperadas.put(variable, String.valueOf(data.get(variable)));
            enlazadas.put(variable, valor(componente, propiedades));
        }
        assertThat(data.keySet()).containsExactlyInAnyOrderElementsOf(esperadas.keySet());
        assertThat(enlazadas).isEqualTo(esperadas);
        assertThat(VALIDADOR.validate(propiedades)).isEmpty();
    }

    @Test
    void certYProdTienenLasMismasClaves() throws IOException {
        assertThat(leerData("deploy/prod/configmap.yaml").keySet())
                .isEqualTo(leerData("deploy/cert/configmap.yaml").keySet());
    }

    @Test
    void rechazaUnaTasaPreferenteQueNoEsMenorQueLaBase() {
        SimuladorProperties propiedades = new SimuladorProperties("PEN", new BigDecimal("0.20"),
                new BigDecimal("0.20"), new BigDecimal("1000"), new BigDecimal("150000"), 6, 60);

        Set<String> mensajes = VALIDADOR.validate(propiedades).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(mensajes).containsExactly(
                "la TEA preferente para clientes con haberes debe ser menor que la TEA base");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> leerData(String rutaConfigMap) throws IOException {
        Map<String, Object> configMap = new Yaml().load(Files.readString(Path.of(rutaConfigMap)));
        return (Map<String, Object>) configMap.get("data");
    }

    private static SimuladorProperties enlazar(Map<String, Object> variablesDeEntorno) {
        // El nombre "systemEnvironment" activa el binding relajado de variables de entorno.
        var fuente = new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variablesDeEntorno);
        return new Binder(ConfigurationPropertySources.from(fuente))
                .bind("cuotafacil.simulador", SimuladorProperties.class)
                .orElseThrow(() -> new AssertionError("el config map no enlazó ninguna propiedad"));
    }

    private static String valor(RecordComponent componente, SimuladorProperties propiedades) {
        try {
            Object valor = componente.getAccessor().invoke(propiedades);
            return valor instanceof BigDecimal decimal ? decimal.toPlainString() : String.valueOf(valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
