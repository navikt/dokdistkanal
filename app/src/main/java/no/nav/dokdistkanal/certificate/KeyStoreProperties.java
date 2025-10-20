package no.nav.dokdistkanal.certificate;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("nav.virksomhetssertifikat")
public record KeyStoreProperties(
		@NotBlank String type,
		@NotBlank String alias,
		@NotBlank String password,
		@NotBlank String key) {
}
