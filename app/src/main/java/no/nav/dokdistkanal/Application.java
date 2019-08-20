package no.nav.dokdistkanal;

import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {
		CxfAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		ValidationAutoConfiguration.class,
		ApplicationConfig.class})
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
