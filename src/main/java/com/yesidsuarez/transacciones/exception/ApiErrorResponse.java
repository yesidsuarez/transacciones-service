package com.yesidsuarez.transacciones.exception;

import java.time.LocalDateTime;

/**
 * Cuerpo estándar de las respuestas de error de la API.
 *
 * @author YesidSuarez
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path) {
}
