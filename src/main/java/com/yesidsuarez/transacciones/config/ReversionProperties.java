package com.yesidsuarez.transacciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 *
 * @param plazoMaximo tiempo máximo, contado desde la fecha de creación, para permitir la reversión
 * @author YesidSuarez
 */
@ConfigurationProperties(prefix = "transacciones.reversion")
public record ReversionProperties(Duration plazoMaximo) {

    public ReversionProperties {
        if (plazoMaximo == null) {
            plazoMaximo = Duration.ofHours(24);
        }
    }
}
