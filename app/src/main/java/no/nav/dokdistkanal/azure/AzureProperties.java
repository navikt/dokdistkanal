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
	public static final String SPRING_DEFAULT_PRINCIPAL = "anonymousUser";
	public static final String CLIENT_REGISTRATION_DIGDIR_KRR_PROXY = "azure-digdir-krr-proxy";
	public static final String CLIENT_REGISTRATION_PDL = "azure-pdl";
	public static final String CLIENT_REGISTRATION_DOKMET = "azure-dokmet";

	public static OAuth2AuthorizeRequest getOAuth2AuthorizeRequestForAzure(String clientRegistrationId) {
		return OAuth2AuthorizeRequest
				.withClientRegistrationId(clientRegistrationId)
				.principal(SPRING_DEFAULT_PRINCIPAL)
				.build();
	}

}

