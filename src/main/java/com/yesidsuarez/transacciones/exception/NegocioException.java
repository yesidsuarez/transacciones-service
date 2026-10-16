package com.yesidsuarez.transacciones.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de negocio. Cada subclase declara el código HTTP que le corresponde,
 * de modo que el manejador global no necesita conocer cada caso concreto
 * (principio Abierto/Cerrado: agregar una regla nueva = agregar una excepción nueva).
 *
 * @author YesidSuarez
 */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String mensaje) {
        super(mensaje);
    }

    protected NegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** Código HTTP con el que debe responderse esta excepción. */
    public abstract HttpStatus getHttpStatus();
}
