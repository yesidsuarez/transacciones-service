package com.yesidsuarez.transacciones.controller;

import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import com.yesidsuarez.transacciones.exception.EstadoInvalidoParaReversionException;
import com.yesidsuarez.transacciones.exception.PlazoReversionExpiradoException;
import com.yesidsuarez.transacciones.exception.TransaccionModificadaConcurrentementeException;
import com.yesidsuarez.transacciones.exception.TransaccionNoEncontradaException;
import com.yesidsuarez.transacciones.service.TransaccionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el {@code GlobalExceptionHandler} traduzca cada excepción al código HTTP correcto.
 *
 * @author YesidSuarez
 */
@WebMvcTest(TransaccionController.class)
@ActiveProfiles("test")
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransaccionService transaccionService;

    @Test
    @DisplayName("204 No Content cuando la reversión es exitosa")
    void reversionExitosa() throws Exception {
        mockMvc.perform(patch("/transacciones/1/reversar"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("409 Conflict cuando el estado no es APROBADA")
    void estadoInvalido() throws Exception {
        doThrow(new EstadoInvalidoParaReversionException(1L, EstadoTransaccion.PENDIENTE))
                .when(transaccionService).reversar(1L);

        mockMvc.perform(patch("/transacciones/1/reversar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/transacciones/1/reversar"));
    }

    @Test
    @DisplayName("422 Unprocessable Entity cuando venció el plazo de 24 horas")
    void plazoVencido() throws Exception {
        doThrow(new PlazoReversionExpiradoException(1L, Duration.ofHours(24)))
                .when(transaccionService).reversar(1L);

        mockMvc.perform(patch("/transacciones/1/reversar"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("409 Conflict cuando hay modificación concurrente")
    void concurrencia() throws Exception {
        doThrow(new TransaccionModificadaConcurrentementeException(1L, null))
                .when(transaccionService).reversar(1L);

        mockMvc.perform(patch("/transacciones/1/reversar"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("404 Not Found cuando la transacción no existe")
    void noEncontrada() throws Exception {
        doThrow(new TransaccionNoEncontradaException(99L)).when(transaccionService).reversar(99L);

        mockMvc.perform(patch("/transacciones/99/reversar"))
                .andExpect(status().isNotFound());
    }
}
