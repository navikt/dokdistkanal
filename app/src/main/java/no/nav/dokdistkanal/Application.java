package no.nav.dokdistkanal;

import no.nav.dokdistkanal.config.RestWebMvcConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(value = {ApplicationConfig.class, RestWebMvcConfig.class})
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
