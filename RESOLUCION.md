# Resolución del caso OCD: CuotaFácil (CF-240 y CF-241)

El caso parte de un microservicio `cuotafacil` que no venía con el material, así que lo creé en este repositorio con Spring Boot 3.5.16 y Java 17. Es el simulador de cuotas en su versión 1.2.0, que agrega la tasa preferente para clientes con haberes. Las piezas del pase son archivos dentro del proyecto y las pruebas del build las validan contra las reglas RN-1 a RN-5.

| Pieza | Archivo |
|---|---|
| Delivery.json de CF-240 | `ocd/CF-240/Delivery.json` |
| Ventanas de ChaMa, checklist y conformidades de CF-240 | `ocd/CF-240/chama.yaml` |
| Delivery.json de CF-241 | `ocd/CF-241/Delivery.json` |
| Catálogo de jobs de Jenkins (datos del caso) | `ocd/catalogo-jobs.json` |
| Config maps de CERT y PROD | `deploy/cert/configmap.yaml`, `deploy/prod/configmap.yaml` |
| Validación automática de RN-2 y RN-3 | `src/test/java/com/cuotafacil/ocd/ReglasOcdTest.java` |

## Paso 1. Plan del pase CF-240

### Recorrido

CF-240 es de criticidad 3, así que recorre los 8 tramos. Una instrucción cambia una credencial, por eso Seguridad entra en Validaciones automatizadas.

| Tramo | Roles | Qué pasa |
|---|---|---|
| 1. Registro | Developer / LT | Se crea CF-240 para cuotafacil 1.2.0 (tag v1.2.0 en master): criticidad 3, solo ejecución de jobs, 100% Automatizado = Sí. El Delivery.json entra ya validado. |
| 2. Diseño y codificación | Developer / LT | Carlos Vega implementa la tasa preferente para clientes con haberes y Lucía Flores hace el code review. |
| 3. Deploy DEV y validación | Developer | Carlos despliega en DEV y comprueba que la app queda estable: health en UP y simulación con y sin haberes. |
| 4. Validaciones automatizadas | LT + Seguridad | Lucía revisa las instrucciones del pase. Martín Rivas (Seguridad) aprueba la instrucción que cambia la credencial. |
| 5. Congelamiento en CERT | AgileOps / LT | Óscar León ejecuta los jobs de CERT del Delivery.json, primero config maps y luego deploy, y espera el fin de ejecución antes de mover el ticket. |
| 6. Pruebas QE y reversión | QE / AgileOps | Paola Díaz prueba en CERT. Se ejecutan los revert de CERT para probar la reversión y se vuelve a congelar. |
| 7. Gestión del pase | LT | Lucía agenda las ventanas en ChaMa y reúne el checklist y las conformidades del pase. |
| 8. Pase a PROD y ratificación | AgileOps / QE | Óscar ejecuta los jobs de PROD el jueves 17/12 a las 22:00. Paola ratifica de 23:00 a 23:45; si no puede, se revierte de 23:45 a 01:00. |

### Delivery.json

Cuatro jobs, cada uno con su revert del mismo grupo y ambiente, y en cada ambiente los config maps antes del deploy:

```json
{
  "componente": "cuotafacil",
  "version": "1.2.0",
  "cert": [
    { "job":    "CUOTAFACIL-CERT/CONFIG_MAPS/01.PIPELINE_CONFIG_MAPS_CERT",
      "revert": "CUOTAFACIL-CERT/CONFIG_MAPS/02.PIPELINE_CONFIG_MAPS_REVERT_CERT" },
    { "job":    "CUOTAFACIL-CERT/DEPLOY/01.PIPELINE_DEPLOY_CERT",
      "revert": "CUOTAFACIL-CERT/DEPLOY/02.PIPELINE_DEPLOY_REVERT_CERT" }
  ],
  "prod": [
    { "job":    "CUOTAFACIL-PROD/CONFIG_MAPS/01.PIPELINE_CONFIG_MAPS_PROD",
      "revert": "CUOTAFACIL-PROD/CONFIG_MAPS/02.PIPELINE_CONFIG_MAPS_REVERT_PROD" },
    { "job":    "CUOTAFACIL-PROD/DEPLOY/01.PIPELINE_DEPLOY_PROD",
      "revert": "CUOTAFACIL-PROD/DEPLOY/02.PIPELINE_DEPLOY_REVERT_PROD" }
  ]
}
```

| Job que queda fuera | Motivo |
|---|---|
| CERT/PERF/01.PIPELINE_PERFORMANCE_CERT | Tiene 52 300 líneas de log y el límite es 30 000. |
| PROD/CACHE/01.PIPELINE_FLUSH_CACHE_PROD | No tiene repositorio en Bitbucket. Además, sumarlo daría 5 jobs. |

### ChaMa, checklist y conformidades

```yaml
chama:
  pase:          jue 17/12/2026  22:00 - 23:00
  ratificacion:  jue 17/12/2026  23:00 - 23:45
  reversion:     jue 17/12/2026  23:45 - vie 18/12/2026 01:00
checklists:          [LT, SEGURIDAD, PO]
conformidadesPase:   [LT, CL-DEV, CL-QA, OPS]
```

El valor de la credencial no aparece en ningún archivo del proyecto ni en este documento. Va en la instrucción que aprueba Seguridad (RN-5).

## Paso 2. Ticket CF-241

CF-241 sube el plazo máximo del préstamo de 60 a 72 meses. En el proyecto ese plazo es la propiedad `cuotafacil.simulador.plazo-maximo-meses`, que en cada ambiente llega desde el config map como `CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES`. No cambia código.

### Tramos

```
Registro -> Deploy DEV y validacion -> Validaciones automatizadas (LT)
         -> Congelamiento en CERT -> Gestion del pase -> Pase a PROD y ratificacion
```

Recorre 6 tramos. Salta Diseño y codificación porque ese tramo es solo de criticidad 2 y 3, y salta Pruebas QE y reversión porque es solo de criticidad 3. En Validaciones automatizadas solo está el LT: ninguna instrucción requiere a Seguridad.

### Delivery.json

Solo los config maps, cada uno con su revert:

```json
{
  "componente": "cuotafacil",
  "version": "1.2.0",
  "cert": [
    { "job":    "CUOTAFACIL-CERT/CONFIG_MAPS/01.PIPELINE_CONFIG_MAPS_CERT",
      "revert": "CUOTAFACIL-CERT/CONFIG_MAPS/02.PIPELINE_CONFIG_MAPS_REVERT_CERT" }
  ],
  "prod": [
    { "job":    "CUOTAFACIL-PROD/CONFIG_MAPS/01.PIPELINE_CONFIG_MAPS_PROD",
      "revert": "CUOTAFACIL-PROD/CONFIG_MAPS/02.PIPELINE_CONFIG_MAPS_REVERT_PROD" }
  ]
}
```

La versión sigue en 1.2.0 porque un ajuste de configuración no cambia la versión del componente.

### Diferencias con CF-240

| Aspecto | CF-240 | CF-241 |
|---|---|---|
| Criticidad | 3 | 1 |
| Tramos | Los 8 | 6: sin Diseño y codificación ni Pruebas QE y reversión |
| Validaciones automatizadas | LT + Seguridad | LT |
| Delivery.json | 4 jobs: config maps y deploy | 2 jobs: solo config maps |
| Prueba de la reversión en CERT | Sí (tramo 6) | No, porque no recorre el tramo 6 |
| Gestión y pase a PROD | ChaMa, conformidades, AgileOps y ratificación de QE | Igual |

### El cambio en los config maps

```diff
-  CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES: "60"
+  CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES: "72"
```

El cambio va en `deploy/cert/configmap.yaml` y en `deploy/prod/configmap.yaml`. No lo apliqué en el proyecto, que queda tal como lo lleva CF-240 (la 1.2.0 con 60 meses). La prueba `PlazoMaximoCf241Test` arranca la app con 72 y comprueba que acepta un préstamo a 72 meses (S/ 20 000 con haberes: cuota de 459.40).

### Dos puntos que el LT debe decidir antes de mover CF-241

1. La app lee el config map al arrancar. El job de config maps actualiza el ConfigMap, pero los pods que ya están corriendo siguen con 60 hasta que se reinician. Si el job CONFIG_MAPS no reinicia los pods, el LT suma los jobs de deploy de CERT y PROD: CF-241 queda con 4 jobs, dentro del máximo. Es el caso que menciona el ejercicio.
2. CF-240 y CF-241 escriben el mismo ConfigMap (`cuotafacil-config`). Las ventanas de CF-241 tienen que empezar después del 18/12 a la 01:00, cuando ya cerró la ventana de reversión de CF-240. Si CF-240 se revirtiera con CF-241 ya aplicado, el revert de config maps de CF-240 devolvería el plazo a su valor anterior. El caso no da fecha para CF-241, así que sus ventanas quedan por agendar.

## Paso 3. Revisión del LT

| Aspecto | Cómo se resolvió | ¿Correcto? | Corrección |
|---|---|---|---|
| Tramos por criticidad | CF-240: 8 tramos. CF-241: 6, sin los tramos 2 y 6. Orden del flujo y roles respetados. | Sí | Ninguna |
| Seguridad | Solo en el tramo 4 de CF-240, por la credencial. CF-241 no la necesita. | Sí | Ninguna |
| Delivery.json | CF-240 con 4 jobs y CF-241 con 2; cada uno con su revert del mismo grupo y ambiente. | Sí | Ninguna |
| Jobs excluidos | PERF por 52 300 líneas de log y CACHE por no tener repositorio en Bitbucket. | Sí | Ninguna |
| Orden por ambiente | Config maps antes del deploy en CERT y en PROD. | Sí | Ninguna |
| ChaMa y conformidades | Pase, ratificación y reversión en orden y sin solaparse. Checklist LT, Seguridad y PO; conformidades LT, CL-DEV, CL-QA y OPS. | Sí | Ninguna |
| Claves del config map | Cada clave de CERT y PROD llega a su propiedad; no sobra ni falta ninguna. | Sí | Ninguna |

Estas comprobaciones no dependen solo de una lectura. `ReglasOcdTest` valida los Delivery.json contra el catálogo de jobs (máximo 4, revert del mismo grupo y ambiente, límite de 30 000 líneas, repositorio en Bitbucket, config maps antes del deploy) y las ventanas de ChaMa. `ConfigMapBindingTest` carga cada config map como variables de entorno del pod y comprueba que llegan a las propiedades. Para confirmar que las pruebas detectan errores, metí dos a propósito: un revert de PROD apuntando al grupo DEPLOY y una clave del config map mal escrita. Las dos pruebas fallaron con el mensaje correcto y después restauré los archivos.

## Paso 4. Mapa de fallos del recorrido de CF-240

| Situación | Tramo | Cómo se resuelve |
|---|---|---|
| Un job del Delivery.json falla | Congelamiento en CERT | Botón «Ejecutar job» (en Acciones) y esperar el mensaje de fin de ejecución. |
| El ticket se mueve con jobs en curso | Cualquier tramo con jobs | Esperar siempre el mensaje de fin de ejecución antes de mover el ticket; si no, el error aparece en el tramo siguiente. |
| Cae una herramienta DevSecOps en la ventana de pase | Pase a PROD y ratificación (también Congelamiento) | Contingencia: ejecutar a mano los jobs del Delivery.json. Es solo para este caso, no para un job que falla. |
| Un job no cumple los requisitos del Delivery.json | Antes del Registro | Corregirlo antes de crear el ticket: repositorio en Bitbucket, log por debajo de 30 000 líneas y máximo 4 jobs. |
| La reversión nunca se probó | Pruebas QE y reversión | En criticidad 3 se prueba la reversión en CERT y se recongela antes del pase. |
| QE no puede ratificar en PROD | Pase a PROD y ratificación | Reversión en su ventana de ChaMa (23:45 a 01:00) y ratificación de la reversión por QE. |
| Seguridad no aprueba la instrucción de la credencial | Validaciones automatizadas | El ticket no pasa a Congelamiento; se corrige la instrucción y se vuelve a pedir la aprobación. |
| Una clave del config map está mal escrita | Antes del Registro | Spring Boot la ignora sin error y la app arranca con el valor por defecto. `ConfigMapBindingTest` la detecta en el build. |

### Qué revisa QE al ratificar

| Ticket | Llamada | Resultado esperado |
|---|---|---|
| CF-240 | `GET /actuator/info` | `build.version` = `1.2.0` |
| CF-240 | `POST /api/v1/simulaciones` con `{"monto": 10000, "plazoMeses": 12, "conHaberes": true}` | `tipoTasa` = `PREFERENTE_HABERES`, `cuotaMensual` = `918.17` |
| CF-241 | `GET /api/v1/condiciones` | `plazoMaximoMeses` = `72` |

## Cómo se verificó

- `./mvnw clean verify` con JDK 21: 26 pruebas, 0 fallos.
- El jar se compila con `release 17`: las clases tienen la versión 61, que es la de Java 17. No lo ejecuté sobre un JDK 17, solo sobre el 21.
- Arranqué el jar dos veces. Con la configuración por defecto, `/api/v1/condiciones` dio 60 meses y una simulación a 72 meses respondió 400. Con `CUOTAFACIL_SIMULADOR_PLAZOMAXIMOMESES=72`, las condiciones dieron 72 y la misma simulación devolvió una cuota de 459.40.
- No construí la imagen del `Dockerfile` ni apliqué los manifiestos de `deploy/` en un clúster.
