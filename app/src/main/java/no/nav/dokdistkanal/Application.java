package no.nav.dokdistkanal;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Import(ApplicationConfig.class)
@SpringBootApplication(exclude = {WebMvcObservationAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
@EnableResilientMethods
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
