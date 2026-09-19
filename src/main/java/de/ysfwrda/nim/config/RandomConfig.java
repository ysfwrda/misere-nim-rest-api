package de.ysfwrda.nim.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
@EnableConfigurationProperties(NimProperties.class)
public class RandomConfig {

    @Bean
    public Random random() {
        return new Random();
    }
}
