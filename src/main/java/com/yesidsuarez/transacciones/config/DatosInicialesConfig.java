package com.yesidsuarez.transacciones.config;

import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import com.yesidsuarez.transacciones.domain.Transaccion;
import com.yesidsuarez.transacciones.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 
 *
 * @author YesidSuarez
 */
@Configuration
@Profile("default")
public class DatosInicialesConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesConfig.class);

    @Bean
    public CommandLineRunner cargarDatosIniciales(TransaccionRepository repository, Clock clock) {
        return args -> {
            LocalDateTime ahora = LocalDateTime.now(clock);
            List<Transaccion> datos = repository.saveAll(List.of(
                    // id 1: APROBADA hace 2 horas      -> reversión exitosa (204)
                    new Transaccion(new BigDecimal("150000.00"), EstadoTransaccion.APROBADA, ahora.minusHours(2)),
                    // id 2: APROBADA hace 30 horas     -> plazo vencido (422)
                    new Transaccion(new BigDecimal("80000.00"), EstadoTransaccion.APROBADA, ahora.minusHours(30)),
                    // id 3: PENDIENTE                  -> estado inválido (409)
                    new Transaccion(new BigDecimal("45000.00"), EstadoTransaccion.PENDIENTE, ahora.minusHours(1)),
                    // id 4: RECHAZADA                  -> estado inválido (409)
                    new Transaccion(new BigDecimal("12000.00"), EstadoTransaccion.RECHAZADA, ahora.minusHours(3)),
                    // id 5: APROBADA hace 23 horas     -> reversión exitosa (204), útil para probar concurrencia
                    new Transaccion(new BigDecimal("990000.00"), EstadoTransaccion.APROBADA, ahora.minusHours(23))
            ));
            log.info("Datos de ejemplo cargados: {} transacciones", datos.size());
        };
    }
}
