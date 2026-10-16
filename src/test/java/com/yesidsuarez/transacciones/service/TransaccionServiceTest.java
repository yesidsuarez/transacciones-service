package com.yesidsuarez.transacciones.service;

import com.yesidsuarez.transacciones.config.ReversionProperties;
import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import com.yesidsuarez.transacciones.domain.Transaccion;
import com.yesidsuarez.transacciones.exception.EstadoInvalidoParaReversionException;
import com.yesidsuarez.transacciones.exception.PlazoReversionExpiradoException;
import com.yesidsuarez.transacciones.exception.TransaccionModificadaConcurrentementeException;
import com.yesidsuarez.transacciones.exception.TransaccionNoEncontradaException;
import com.yesidsuarez.transacciones.repository.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link TransaccionService} con Mockito.
 * El reloj se fija en un instante conocido para que la regla de 24 horas sea determinista.
 *
 * @author YesidSuarez
 */
@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** "Ahora" fijo para todas las pruebas: 16-oct-2026 12:00 hora Bogotá. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 12, 0);
    private static final Long ID = 1L;

    @Mock
    private TransaccionRepository transaccionRepository;

    private TransaccionService transaccionService;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        transaccionService = new TransaccionService(
                transaccionRepository, relojFijo, new ReversionProperties(Duration.ofHours(24)));
    }

    private Transaccion transaccion(EstadoTransaccion estado, LocalDateTime fechaCreacion) {
        Transaccion t = new Transaccion(new BigDecimal("100000.00"), estado, fechaCreacion);
        t.setId(ID);
        t.setVersion(0L);
        return t;
    }

    @Nested
    @DisplayName("Regla de las 24 horas")
    class ReglaVeinticuatroHoras {

        @Test
        @DisplayName("Reversa y persiste cuando la transacción APROBADA tiene menos de 24 horas")
        void reversaDentroDelPlazo() {
            Transaccion t = transaccion(EstadoTransaccion.APROBADA, AHORA.minusHours(23).minusMinutes(59));
            when(transaccionRepository.findById(ID)).thenReturn(Optional.of(t));

            transaccionService.reversar(ID);

            ArgumentCaptor<Transaccion> captor = ArgumentCaptor.forClass(Transaccion.class);
            verify(transaccionRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoTransaccion.REVERSADA);
        }

        @Test
        @DisplayName("Permite reversar exactamente en el límite de 24 horas")
        void reversaEnElLimiteExacto() {
            Transaccion t = transaccion(EstadoTransaccion.APROBADA, AHORA.minusHours(24));
            when(transaccionRepository.findById(ID)).thenReturn(Optional.of(t));

            transaccionService.reversar(ID);

            assertThat(t.getEstado()).isEqualTo(EstadoTransaccion.REVERSADA);
            verify(transaccionRepository).saveAndFlush(t);
        }

        @Test
        @DisplayName("Lanza PlazoReversionExpiradoException (422) cuando pasaron más de 24 horas")
        void rechazaFueraDelPlazo() {
            Transaccion t = transaccion(EstadoTransaccion.APROBADA, AHORA.minusHours(24).minusSeconds(1));
            when(transaccionRepository.findById(ID)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> transaccionService.reversar(ID))
                    .isInstanceOf(PlazoReversionExpiradoException.class)
                    .hasMessageContaining("24 horas");

            assertThat(t.getEstado()).isEqualTo(EstadoTransaccion.APROBADA);
            verify(transaccionRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Regla de estado")
    class ReglaEstado {

        @ParameterizedTest(name = "Estado {0} -> 409 Conflict")
        @EnumSource(value = EstadoTransaccion.class, names = "APROBADA", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Lanza EstadoInvalidoParaReversionException (409) si el estado no es APROBADA")
        void rechazaEstadoDistintoDeAprobada(EstadoTransaccion estado) {
            Transaccion t = transaccion(estado, AHORA.minusHours(1));
            when(transaccionRepository.findById(ID)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> transaccionService.reversar(ID))
                    .isInstanceOf(EstadoInvalidoParaReversionException.class)
                    .hasMessageContaining(estado.name());

            verify(transaccionRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    @DisplayName("Lanza TransaccionNoEncontradaException (404) si la transacción no existe")
    void transaccionInexistente() {
        when(transaccionRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.reversar(ID))
                .isInstanceOf(TransaccionNoEncontradaException.class);
    }

    @Test
    @DisplayName("Traduce el conflicto de bloqueo optimista a TransaccionModificadaConcurrentementeException (409)")
    void conflictoDeConcurrencia() {
        Transaccion t = transaccion(EstadoTransaccion.APROBADA, AHORA.minusHours(1));
        when(transaccionRepository.findById(ID)).thenReturn(Optional.of(t));
        when(transaccionRepository.saveAndFlush(t))
                .thenThrow(new ObjectOptimisticLockingFailureException(Transaccion.class, ID));

        assertThatThrownBy(() -> transaccionService.reversar(ID))
                .isInstanceOf(TransaccionModificadaConcurrentementeException.class)
                .hasMessageContaining("modificada concurrentemente");
    }
}
