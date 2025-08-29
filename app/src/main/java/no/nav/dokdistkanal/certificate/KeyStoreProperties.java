package no.nav.dokdistkanal.certificate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("virksomhetssertifikat")
public record KeyStoreProperties(
		@NotBlank String type,
		@NotBlank String alias,
		@NotBlank String password,
		@NotNull Resource path) {

}
