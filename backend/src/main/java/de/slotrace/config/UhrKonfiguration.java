package de.slotrace.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UhrKonfiguration {

    @Bean
    Clock uhr() {
        return Clock.systemUTC();
    }
}
