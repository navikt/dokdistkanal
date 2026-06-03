package no.nav.dokdistkanal.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("maskinporten")
public class MaskinportenProperties {
	@NotEmpty
	private String issuer;
	@NotEmpty
	private String tokenEndpoint;
}
