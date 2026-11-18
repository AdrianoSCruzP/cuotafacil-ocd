# cuotafacil

Simulador de cuotas de préstamos personales (sistema francés, cuota fija mensual). La versión 1.2.0 agrega una tasa preferente para clientes que reciben sus haberes en el banco. Spring Boot 3.5.16, Java 17.

## API

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/v1/simulaciones` | Calcula la cuota. Cuerpo: `{"monto": 10000, "plazoMeses": 12, "conHaberes": true}` |
| GET | `/api/v1/condiciones` | Devuelve las tasas, montos y plazos que cargó el ambiente |
| GET | `/actuator/health`, `/actuator/info` | Estado y versión (`build.version`) |

Respuesta de una simulación:

```json
{"moneda":"PEN","monto":10000.00,"plazoMeses":12,"tipoTasa":"PREFERENTE_HABERES",
 "teaPorcentaje":19.90,"temPorcentaje":1.5239,"cuotaMensual":918.17,
 "totalAPagar":11018.04,"totalIntereses":1018.04}
```

Un monto o plazo fuera de rango responde 400 con un `ProblemDetail`, por ejemplo `"detail": "El plazo debe estar entre 6 y 60 meses"`. Si falta un campo o tiene un formato inválido, el detalle va en `errores`, por campo.

## Configuración

En local se usa `application.yml`. En CERT y PROD los valores llegan desde el config map `cuotafacil-config` (`deploy/<ambiente>/configmap.yaml`) como variables de entorno.

| Propiedad | Variable del config map | Valor en 1.2.0 |
|---|---|---|
| `cuotafacil.simulador.moneda` | `CUOTAFACIL_SIMULADOR_MONEDA` | PEN |
| `cuotafacil.simulador.tea-base` | `CUOTAFACIL_SIMULADOR_TEABASE` | 0.2490 |
| `cuotafacil.simulador.tea-preferente-haberes` | `CUOTAFACIL_SIMULADOR_TEAPREFERENTEHABERES` | 0.1990 |
| `cuotafacil.simulador.monto-minimo` | `CUOTAFACIL_SIMULADOR_MONTOMINIMO` | 1000 |
| `cuotafacil.simulador.monto-maximo` | `CUOTAFACIL_SIMULADOR_MONTOMAXIMO` | 150000 |
| `cuotafacil.simulador.plazo-minimo-meses` | `CUOTAFACIL_SIMULADOR_PLAZOMINIMOMESES` | 6 |
| `cuotafacil.simulador.plazo-maximo-meses` | `CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES` | 60 |

Las tasas son valores de ejemplo para el caso. La app valida la configuración al arrancar: no levanta si la TEA preferente no es menor que la base o si un rango está invertido. Las propiedades se leen solo al arrancar, así que un config map nuevo llega a la app cuando se reinician los pods.

## Comandos

```bash
# requiere JDK 17 o superior en JAVA_HOME
./mvnw clean verify                  # compila para Java 17 y corre las 26 pruebas
java -jar target/cuotafacil.jar      # arranca en :8080 (SERVER_PORT para cambiarlo)
```

## Estructura

```
src/main/java/com/cuotafacil/
  config/SimuladorProperties.java      propiedades validadas
  simulacion/                          controlador, servicio de cálculo, DTO y manejo de errores
deploy/                                config maps de CERT y PROD y el Deployment
ocd/                                   Delivery.json y ChaMa de CF-240 y CF-241, catálogo de jobs
src/test/java/com/cuotafacil/
  simulacion/                          cálculo, API y el plazo de 72 meses de CF-241
  config/ConfigMapBindingTest.java     cada clave del config map llega a su propiedad
  ocd/ReglasOcdTest.java               Delivery.json contra RN-2 y ventanas de ChaMa contra RN-3
```

La resolución completa del caso está en `RESOLUCION.md`.
