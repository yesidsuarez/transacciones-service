package com.yesidsuarez.transacciones.service;

import com.yesidsuarez.transacciones.config.ReversionProperties;
import com.yesidsuarez.transacciones.domain.Transaccion;
import com.yesidsuarez.transacciones.exception.EstadoInvalidoParaReversionException;
import com.yesidsuarez.transacciones.exception.PlazoReversionExpiradoException;
import com.yesidsuarez.transacciones.exception.TransaccionModificadaConcurrentementeException;
import com.yesidsuarez.transacciones.exception.TransaccionNoEncontradaException;
import com.yesidsuarez.transacciones.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio de las transacciones.
 *
 * <h2>Reglas de reversión</h2>
 * <ol>
 *   <li>Solo se reversa una transacción en estado APROBADA; si no, 409 Conflict.</li>
 *   <li>Solo dentro de las 24 h siguientes a {@code fechaCreacion}; si no, 422 Unprocessable Entity.</li>
 *   <li>Dos reversiones simultáneas no pueden tener éxito: el bloqueo optimista
 *       ({@code @Version}) hace fallar a la segunda con 409 Conflict.</li>
 * </ol>
 *
 * @author YesidSuarez
 */
@Service
public class TransaccionService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionService.class);

    private final TransaccionRepository transaccionRepository;
    private final Clock clock;
    private final Duration plazoMaximoReversion;

    public TransaccionService(TransaccionRepository transaccionRepository,
                              Clock clock,
                              ReversionProperties reversionProperties) {
        this.transaccionRepository = transaccionRepository;
        this.clock = clock;
        this.plazoMaximoReversion = reversionProperties.plazoMaximo();
    }

    /**
     * Reversa la transacción indicada aplicando las reglas de negocio.
     *
     * @param id identificador de la transacción
     * @throws TransaccionNoEncontradaException                si no existe (404)
     * @throws EstadoInvalidoParaReversionException            si no está APROBADA (409)
     * @throws PlazoReversionExpiradoException                 si pasaron más de 24 h (422)
     * @throws TransaccionModificadaConcurrentementeException  si hubo una modificación concurrente (409)
     */
    @Transactional
    public void reversar(Long id) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new TransaccionNoEncontradaException(id));

        validarEstado(transaccion);
        validarPlazo(transaccion);

        transaccion.marcarComoReversada();

        try {
            // saveAndFlush fuerza el UPDATE ... WHERE version = ? en este momento,
            // así el conflicto de concurrencia se detecta aquí y se traduce a una excepción de negocio.
            transaccionRepository.saveAndFlush(transaccion);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new TransaccionModificadaConcurrentementeException(id, ex);
        }

        log.info("Transacción {} reversada correctamente", id);
    }

    /** Consulta una transacción por id (apoyo para verificar el resultado de la reversión). */
    @Transactional(readOnly = true)
    public Transaccion consultar(Long id) {
        return transaccionRepository.findById(id)
                .orElseThrow(() -> new TransaccionNoEncontradaException(id));
    }

    /** Lista todas las transacciones (apoyo para pruebas manuales). */
    @Transactional(readOnly = true)
    public List<Transaccion> listar() {
        return transaccionRepository.findAll();
    }

    private void validarEstado(Transaccion transaccion) {
        if (!transaccion.estaAprobada()) {
            throw new EstadoInvalidoParaReversionException(transaccion.getId(), transaccion.getEstado());
        }
    }

    private void validarPlazo(Transaccion transaccion) {
        LocalDateTime limite = transaccion.getFechaCreacion().plus(plazoMaximoReversion);
        if (LocalDateTime.now(clock).isAfter(limite)) {
            throw new PlazoReversionExpiradoException(transaccion.getId(), plazoMaximoReversion);
        }
    }
}
