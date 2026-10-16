package com.yesidsuarez.transacciones.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Se lanza cuando ya pasó el plazo permitido para reversar la transacción (HTTP 422).
 *
 * @author YesidSuarez
 */
public class PlazoReversionExpiradoException extends NegocioException {

    public PlazoReversionExpiradoException(Long id, Duration plazo) {
        super("La transacción " + id + " no puede reversarse porque superó el plazo máximo de "
                + plazo.toHours() + " horas desde su fecha de creación.");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
