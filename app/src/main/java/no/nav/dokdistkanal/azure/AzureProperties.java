package no.nav.dokdistkanal.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.validation.annotation.Validated;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@ConfigurationProperties("azure")
@Validated
public record AzureProperties (
	@NotEmpty String openidConfigTokenEndpoint,
	@NotEmpty String appClientId,
	@NotEmpty String appClientSecret
) {
	public static final String CLIENT_REGISTRATION_DIGDIR_KRR_PROXY = "azure-digdir-krr-proxy";
	public static final String CLIENT_REGISTRATION_PDL = "azure-pdl";
}

