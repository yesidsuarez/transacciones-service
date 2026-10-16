package com.yesidsuarez.transacciones.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando otra solicitud modificó la transacción al mismo tiempo
 * (conflicto de bloqueo optimista, HTTP 409).
 *
 * @author YesidSuarez
 */
public class TransaccionModificadaConcurrentementeException extends NegocioException {

    public TransaccionModificadaConcurrentementeException(Long id, Throwable causa) {
        super("La transacción " + id + " fue modificada concurrentemente por otra solicitud. "
                + "Consulte su estado actual e intente de nuevo si aplica.", causa);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
