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

/**
 * CF-241: el plazo máximo pasa de 60 a 72 meses solo con configuración, sin tocar código.
 * Que la clave del config map llegue a esta propiedad lo comprueba {@code ConfigMapBindingTest}.
 */
@SpringBootTest(properties = "cuotafacil.simulador.plazo-maximo-meses=72")
@AutoConfigureMockMvc
class PlazoMaximoCf241Test {

    @Autowired
    private MockMvc mvc;

    @Test
    void aceptaUnPrestamoA72Meses() throws Exception {
        mvc.perform(post("/api/v1/simulaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 20000, "plazoMeses": 72, "conHaberes": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazoMeses").value(72))
                .andExpect(jsonPath("$.cuotaMensual").value(459.40));
    }

    @Test
    void lasCondicionesMuestranElNuevoMaximo() throws Exception {
        mvc.perform(get("/api/v1/condiciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazoMaximoMeses").value(72));
    }
}
