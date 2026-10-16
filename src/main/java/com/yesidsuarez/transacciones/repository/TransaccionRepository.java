package com.yesidsuarez.transacciones.repository;

import com.yesidsuarez.transacciones.domain.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de acceso a datos de {@link Transaccion}.
 *
 * @author YesidSuarez
 */
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
}
