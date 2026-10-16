package com.yesidsuarez.transacciones.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando no existe una transacción con el id solicitado (HTTP 404).
 *
 * @author YesidSuarez
 */
public class TransaccionNoEncontradaException extends NegocioException {

    public TransaccionNoEncontradaException(Long id) {
        super("No existe una transacción con id " + id + ".");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
