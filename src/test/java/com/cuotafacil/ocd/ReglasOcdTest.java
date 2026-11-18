package com.cuotafacil.ocd;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

/**
 * Las comprobaciones que hace el LT antes de mover el ticket (paso 3 del caso), automatizadas
 * sobre los archivos de {@code ocd/}: Delivery.json contra RN-2 y ventanas de ChaMa contra RN-3.
 */
class ReglasOcdTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAXIMO_JOBS = 4;
    private static final int MAXIMO_LINEAS_LOG = 30_000;
    private static final Pattern JOB =
            Pattern.compile("^CUOTAFACIL-(CERT|PROD)/([A-Z_]+)/01\\.PIPELINE_([A-Z_]+)_(CERT|PROD)$");
    private static final Pattern REVERT =
            Pattern.compile("^CUOTAFACIL-(CERT|PROD)/([A-Z_]+)/02\\.PIPELINE_([A-Z_]+)_REVERT_(CERT|PROD)$");

    private record Entrada(String ambiente, String job, String revert) {
    }

    @ParameterizedTest
    @ValueSource(strings = {"ocd/CF-240/Delivery.json", "ocd/CF-241/Delivery.json"})
    void cadaJobCumpleRn2ConSuRevertDelMismoGrupoYAmbiente(String ruta) throws IOException {
        List<Entrada> entradas = leerDelivery(ruta);
        Map<String, JsonNode> catalogo = catalogo();

        assertThat(entradas).hasSizeBetween(1, MAXIMO_JOBS);
        assertThat(entradas).extracting(Entrada::job).doesNotHaveDuplicates();
        for (Entrada e : entradas) {
            Matcher job = JOB.matcher(e.job());
            Matcher revert = REVERT.matcher(e.revert());
            assertThat(job.matches()).as("%s debe ser un job de ejecución 01.PIPELINE_", e.job()).isTrue();
            assertThat(revert.matches()).as("%s debe ser una reversión 02.PIPELINE_..._REVERT_", e.revert()).isTrue();

            String ambiente = e.ambiente().toUpperCase(Locale.ROOT);
            assertThat(job.group(1)).as("ambiente del job %s", e.job()).isEqualTo(ambiente);
            assertThat(job.group(4)).as("sufijo del job %s", e.job()).isEqualTo(ambiente);
            assertThat(revert.group(1)).as("ambiente del revert de %s", e.job()).isEqualTo(ambiente);
            assertThat(revert.group(4)).as("sufijo del revert de %s", e.job()).isEqualTo(ambiente);
            assertThat(revert.group(2)).as("grupo del revert de %s", e.job()).isEqualTo(job.group(2));
            assertThat(revert.group(3)).as("pipeline del revert de %s", e.job()).isEqualTo(job.group(3));

            assertThat(incumplimientos(e.job(), catalogo)).as("requisitos de %s", e.job()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ocd/CF-240/Delivery.json", "ocd/CF-241/Delivery.json"})
    void enCadaAmbienteLosConfigMapsVanAntesDelDeploy(String ruta) throws IOException {
        for (Map.Entry<String, List<String>> ambiente : gruposPorAmbiente(ruta).entrySet()) {
            List<String> grupos = ambiente.getValue();
            if (grupos.contains("DEPLOY") && grupos.contains("CONFIG_MAPS")) {
                assertThat(grupos.indexOf("CONFIG_MAPS")).as(ambiente.getKey()).isLessThan(grupos.indexOf("DEPLOY"));
            }
        }
    }

    @Test
    void cf240LlevaConfigMapsYDeployEnCertYProd() throws IOException {
        JsonNode delivery = JSON.readTree(Path.of("ocd/CF-240/Delivery.json").toFile());

        assertThat(delivery.path("componente").asText()).isEqualTo("cuotafacil");
        assertThat(delivery.path("version").asText()).isEqualTo("1.2.0");
        assertThat(gruposPorAmbiente("ocd/CF-240/Delivery.json")).containsExactly(
                Map.entry("cert", List.of("CONFIG_MAPS", "DEPLOY")),
                Map.entry("prod", List.of("CONFIG_MAPS", "DEPLOY")));
    }

    @Test
    void cf241SoloLlevaLosConfigMaps() throws IOException {
        assertThat(gruposPorAmbiente("ocd/CF-241/Delivery.json")).containsExactly(
                Map.entry("cert", List.of("CONFIG_MAPS")),
                Map.entry("prod", List.of("CONFIG_MAPS")));
    }

    @Test
    void losJobsQueQuedanFueraIncumplenRn2YLosDemasEntranTodosEnCf240() throws IOException {
        Map<String, JsonNode> catalogo = catalogo();

        assertThat(incumplimientos("CUOTAFACIL-CERT/PERF/01.PIPELINE_PERFORMANCE_CERT", catalogo))
                .containsExactly("52300 líneas de log: supera las 30000");
        assertThat(incumplimientos("CUOTAFACIL-PROD/CACHE/01.PIPELINE_FLUSH_CACHE_PROD", catalogo))
                .containsExactly("sin repositorio en Bitbucket");

        List<String> aptos = catalogo.keySet().stream()
                .filter(job -> incumplimientos(job, catalogo).isEmpty())
                .toList();
        assertThat(leerDelivery("ocd/CF-240/Delivery.json")).extracting(Entrada::job)
                .containsExactlyInAnyOrderElementsOf(aptos);
    }

    @Test
    void ventanasDeChamaDeCf240EnOrdenYSinSolaparse() throws IOException {
        Map<String, Object> chama = leerYaml("ocd/CF-240/chama.yaml");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> ventanas = (Map<String, Map<String, String>>) chama.get("chama");

        assertThat(ventanas.keySet()).containsExactly("pase", "ratificacion", "reversion");
        LocalDateTime finAnterior = null;
        for (Map.Entry<String, Map<String, String>> ventana : ventanas.entrySet()) {
            LocalDateTime inicio = LocalDateTime.parse(ventana.getValue().get("inicio"));
            LocalDateTime fin = LocalDateTime.parse(ventana.getValue().get("fin"));
            assertThat(inicio).as("ventana %s", ventana.getKey()).isBefore(fin);
            if (finAnterior != null) {
                assertThat(inicio).as("%s no se solapa con la anterior", ventana.getKey()).isAfterOrEqualTo(finAnterior);
            }
            finAnterior = fin;
        }

        LocalDateTime inicioPase = LocalDateTime.parse(ventanas.get("pase").get("inicio"));
        assertThat(inicioPase).isEqualTo(LocalDateTime.of(2026, 12, 17, 22, 0));
        assertThat(inicioPase.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
    }

    @Test
    void checklistYConformidadesDelPaseNoSeMezclan() throws IOException {
        Map<String, Object> chama = leerYaml("ocd/CF-240/chama.yaml");

        assertThat(chama.get("checklists")).isEqualTo(List.of("LT", "SEGURIDAD", "PO"));
        assertThat(chama.get("conformidadesPase")).isEqualTo(List.of("LT", "CL-DEV", "CL-QA", "OPS"));
    }

    private static List<String> incumplimientos(String job, Map<String, JsonNode> catalogo) {
        JsonNode datos = catalogo.get(job);
        if (datos == null) {
            return List.of("no está en el catálogo de Jenkins");
        }
        List<String> motivos = new ArrayList<>();
        int lineas = datos.path("lineasLog").asInt();
        if (lineas > MAXIMO_LINEAS_LOG) {
            motivos.add(lineas + " líneas de log: supera las " + MAXIMO_LINEAS_LOG);
        }
        if (!datos.path("bitbucket").asBoolean()) {
            motivos.add("sin repositorio en Bitbucket");
        }
        return motivos;
    }

    private static Map<String, JsonNode> catalogo() throws IOException {
        JsonNode raiz = JSON.readTree(Path.of("ocd/catalogo-jobs.json").toFile());
        String prefijo = raiz.path("prefijo").asText();
        Map<String, JsonNode> jobs = new LinkedHashMap<>();
        raiz.path("jobs").forEach(job -> jobs.put(prefijo + job.path("job").asText(), job));
        return jobs;
    }

    private static List<Entrada> leerDelivery(String ruta) throws IOException {
        JsonNode delivery = JSON.readTree(Path.of(ruta).toFile());
        List<Entrada> entradas = new ArrayList<>();
        delivery.fields().forEachRemaining(campo -> {
            if (campo.getValue().isArray()) {
                campo.getValue().forEach(e -> entradas.add(
                        new Entrada(campo.getKey(), e.path("job").asText(), e.path("revert").asText())));
            }
        });
        return entradas;
    }

    private static Map<String, List<String>> gruposPorAmbiente(String ruta) throws IOException {
        Map<String, List<String>> grupos = new LinkedHashMap<>();
        for (Entrada e : leerDelivery(ruta)) {
            Matcher job = JOB.matcher(e.job());
            assertThat(job.matches()).as(e.job()).isTrue();
            grupos.computeIfAbsent(e.ambiente(), k -> new ArrayList<>()).add(job.group(2));
        }
        return grupos;
    }

    private static Map<String, Object> leerYaml(String ruta) throws IOException {
        try (var lector = java.nio.file.Files.newBufferedReader(Path.of(ruta))) {
            return new Yaml().load(lector);
        }
    }
}
