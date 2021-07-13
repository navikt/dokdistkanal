package no.nav.dokdistkanal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {
		ValidationAutoConfiguration.class,
		ApplicationConfig.class})
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		// Lettuce-spring boot interaksjon. Se https://github.com/lettuce-io/lettuce-core/issues/1767
		System.setProperty("io.lettuce.core.jfr", "false");
		SpringApplication.run(Application.class, args);
	}
}
