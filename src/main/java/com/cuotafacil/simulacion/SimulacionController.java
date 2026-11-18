package com.cuotafacil.simulacion;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SimulacionController {

    private final SimuladorCuotaService simulador;

    public SimulacionController(SimuladorCuotaService simulador) {
        this.simulador = simulador;
    }

    /** Simula la cuota; no guarda nada, por eso responde 200 y no 201. */
    @PostMapping("/simulaciones")
    public SimulacionResponse simular(@Valid @RequestBody SimulacionRequest solicitud) {
        return simulador.simular(solicitud.monto(), solicitud.plazoMeses(), solicitud.conHaberes());
    }

    @GetMapping("/condiciones")
    public CondicionesResponse condiciones() {
        return simulador.condiciones();
    }
}
