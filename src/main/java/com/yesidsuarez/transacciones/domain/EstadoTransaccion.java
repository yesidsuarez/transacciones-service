package com.yesidsuarez.transacciones.domain;

/**
 * Estados posibles del ciclo de vida de una transacción.
 *
 * @author YesidSuarez
 */
public enum EstadoTransaccion {
    CREADA, PENDIENTE, APROBADA, RECHAZADA, REVERSADA
}
