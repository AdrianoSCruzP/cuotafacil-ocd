package com.cuotafacil.simulacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SimulacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void simulaConTasaPreferente() throws Exception {
        mvc.perform(post("/api/v1/simulaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 10000, "plazoMeses": 12, "conHaberes": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("PEN"))
                .andExpect(jsonPath("$.tipoTasa").value("PREFERENTE_HABERES"))
                .andExpect(jsonPath("$.teaPorcentaje").value(19.90))
                .andExpect(jsonPath("$.cuotaMensual").value(918.17))
                .andExpect(jsonPath("$.totalAPagar").value(11018.04));
    }

    @Test
    void conLaConfiguracionDe60MesesRechaza72() throws Exception {
        mvc.perform(post("/api/v1/simulaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 20000, "plazoMeses": 72, "conHaberes": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Simulación inválida"))
                .andExpect(jsonPath("$.detail").value("El plazo debe estar entre 6 y 60 meses"));
    }

    @Test
    void validaLosCamposObligatorios() throws Exception {
        mvc.perform(post("/api/v1/simulaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 10000.123, "plazoMeses": 12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.conHaberes").value("es obligatorio"))
                .andExpect(jsonPath("$.errores.monto").value("admite hasta 2 decimales"));
    }

    @Test
    void publicaLasCondicionesVigentes() throws Exception {
        mvc.perform(get("/api/v1/condiciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teaBasePorcentaje").value(24.90))
                .andExpect(jsonPath("$.teaPreferenteHaberesPorcentaje").value(19.90))
                .andExpect(jsonPath("$.plazoMaximoMeses").value(60));
    }
}
