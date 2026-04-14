package no.nav.dokdistkanal.config.nais;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("nais")
@Validated
public record NaisProperties(@NotBlank String tokenEndpoint) {
}
