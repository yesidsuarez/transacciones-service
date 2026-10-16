package com.yesidsuarez.transacciones.integration;

import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import com.yesidsuarez.transacciones.domain.Transaccion;
import com.yesidsuarez.transacciones.exception.NegocioException;
import com.yesidsuarez.transacciones.repository.TransaccionRepository;
import com.yesidsuarez.transacciones.service.TransaccionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración (Spring + JPA + H2 reales) del control de concurrencia.
 *
 * @author YesidSuarez
 */
@SpringBootTest
@ActiveProfiles("test")
class ReversionConcurrenteIntegrationTest {

    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @AfterEach
    void limpiar() {
        transaccionRepository.deleteAll();
    }

    private Transaccion crearAprobada() {
        return transaccionRepository.save(new Transaccion(
                new BigDecimal("50000.00"), EstadoTransaccion.APROBADA, LocalDateTime.now().minusHours(1)));
    }

    @Test
    @DisplayName("@Version: guardar una copia desactualizada lanza excepción de bloqueo optimista")
    void versionDesactualizadaFalla() {
        Transaccion original = crearAprobada();
        Transaccion copiaA = transaccionRepository.findById(original.getId()).orElseThrow();
        Transaccion copiaB = transaccionRepository.findById(original.getId()).orElseThrow();

        copiaA.marcarComoReversada();
        transaccionRepository.saveAndFlush(copiaA);           // versión 0 -> 1

        copiaB.marcarComoReversada();                          // copiaB todavía cree que es versión 0
        assertThatThrownBy(() -> transaccionRepository.saveAndFlush(copiaB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Dos reversiones simultáneas: exactamente una tiene éxito y la otra recibe 409")
    void soloUnaReversionConcurrenteTieneExito() throws Exception {
        Long id = crearAprobada().getId();
        int hilos = 2;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(hilos);

        try {
            List<Future<HttpStatus>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                Callable<HttpStatus> tarea = () -> {
                    salida.await();
                    try {
                        transaccionService.reversar(id);
                        return HttpStatus.NO_CONTENT;
                    } catch (NegocioException ex) {
                        return ex.getHttpStatus();
                    }
                };
                resultados.add(executor.submit(tarea));
            }
            salida.countDown(); // ambas solicitudes arrancan al mismo tiempo

            List<HttpStatus> codigos = new ArrayList<>();
            for (Future<HttpStatus> f : resultados) {
                codigos.add(f.get());
            }

            assertThat(codigos).containsOnlyOnce(HttpStatus.NO_CONTENT);
            assertThat(codigos).containsOnlyOnce(HttpStatus.CONFLICT);
            assertThat(transaccionRepository.findById(id).orElseThrow().getEstado())
                    .isEqualTo(EstadoTransaccion.REVERSADA);
        } finally {
            executor.shutdownNow();
        }
    }
}
