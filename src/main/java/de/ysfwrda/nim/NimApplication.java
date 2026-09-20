package de.ysfwrda.nim;

import de.ysfwrda.nim.config.NimProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NimProperties.class)
public class NimApplication {

	public static void main(String[] args) {
		SpringApplication.run(NimApplication.class, args);
	}

}
