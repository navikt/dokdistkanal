package no.nav.dokdistkanal;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Import(ApplicationConfig.class)
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
@EnableResilientMethods
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
