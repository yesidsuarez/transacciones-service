package com.yesidsuarez.transacciones.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

/**
 *
 * @author YesidSuarez
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Todas las reglas de negocio: cada excepción trae su propio código HTTP. */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiErrorResponse> manejarNegocio(NegocioException ex, HttpServletRequest request) {
        LOG.warn("Regla de negocio incumplida: {}", ex.getMessage());
        return construir(ex.getHttpStatus(), ex.getMessage(), request);
    }

    /*
     * (por ejemplo, al hacer commit), igualmente se responde 409.
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiErrorResponse> manejarBloqueoOptimista(Exception ex, HttpServletRequest request) {
        LOG.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return construir(HttpStatus.CONFLICT,
                "La transacción fue modificada concurrentemente por otra solicitud. "
                        + "Consulte su estado actual e intente de nuevo si aplica.",
                request);
    }

    /** Id con formato inválido, por ejemplo /transacciones/abc/reversar: 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue(), request);
    }

    /** Cualquier error no previsto: 500 sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarGenerico(Exception ex, HttpServletRequest request) {
        LOG.error("Error inesperado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente más tarde.", request);
    }

    private ResponseEntity<ApiErrorResponse> construir(HttpStatus status, String mensaje, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
