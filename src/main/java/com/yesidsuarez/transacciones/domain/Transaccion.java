package com.yesidsuarez.transacciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 *
 * @author YesidSuarez
 */
@Entity
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Version
    private Long version;

    protected Transaccion() {
        // Requerido por JPA
    }

    public Transaccion(BigDecimal monto, EstadoTransaccion estado, LocalDateTime fechaCreacion) {
        this.monto = monto;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    /** Indica si la transacción está en un estado que permite reversarla. */
    public boolean estaAprobada() {
        return EstadoTransaccion.APROBADA == estado;
    }

    /** Cambia el estado de la transacción a  EstadoTransaccion #REVERSADA. */
    public void marcarComoReversada() {
        this.estado = EstadoTransaccion.REVERSADA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public void setEstado(EstadoTransaccion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
