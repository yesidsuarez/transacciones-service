package com.yesidsuarez.transacciones.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 *
 *
 * @author YesidSuarez
 */
@Configuration
@EnableConfigurationProperties(ReversionProperties.class)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
