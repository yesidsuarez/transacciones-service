package com.yesidsuarez.transacciones.exception;

import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se intenta reversar una transacción que no está APROBADA (HTTP 409).
 *
 * @author YesidSuarez
 */
public class EstadoInvalidoParaReversionException extends NegocioException {

    public EstadoInvalidoParaReversionException(Long id, EstadoTransaccion estadoActual) {
        super("La transacción " + id + " no puede reversarse porque su estado actual es "
                + estadoActual + ". Solo se pueden reversar transacciones en estado APROBADA.");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
