package com.yesidsuarez.transacciones.controller;

import com.yesidsuarez.transacciones.domain.EstadoTransaccion;
import com.yesidsuarez.transacciones.domain.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida: evita exponer la entidad JPA directamente en la API.
 *
 * @author YesidSuarez
 */
public record TransaccionResponse(
        Long id,
        BigDecimal monto,
        EstadoTransaccion estado,
        LocalDateTime fechaCreacion,
        Long version) {

    public static TransaccionResponse desde(Transaccion t) {
        return new TransaccionResponse(t.getId(), t.getMonto(), t.getEstado(), t.getFechaCreacion(), t.getVersion());
    }
}
