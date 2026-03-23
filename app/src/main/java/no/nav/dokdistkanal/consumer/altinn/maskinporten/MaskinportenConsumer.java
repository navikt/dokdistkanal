package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.dokdistkanal.certificate.AppCertificate;
import no.nav.dokdistkanal.certificate.KeyStoreProperties;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.MaskinportenTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Date.from;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.constants.DomainConstants.DEFAULT_ZONE_ID;
import static no.nav.dokdistkanal.constants.DomainConstants.NAV_ORGNUMMER;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.generateSignedJWTFromCertificate;

@Component
public class MaskinportenConsumer {

	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final RestClient restClient;
	private final MaskinportenProperties maskinportenProperties;
	private final AppCertificate appCertificate;
	private final DokdistkanalProperties.Dpo dpoProperties;

	public MaskinportenConsumer(RestClient.Builder restClientBuilder,
								MaskinportenProperties maskinportenProperties,
								KeyStoreProperties keyStoreProperties,
								DokdistkanalProperties dokdistkanalProperties) {
		this.maskinportenProperties = maskinportenProperties;
		this.appCertificate = new AppCertificate(keyStoreProperties);
		this.dpoProperties = dokdistkanalProperties.getDpo();
		this.restClient = restClientBuilder
				.baseUrl(maskinportenProperties.getTokenEndpoint())
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	@Cacheable(MASKINPORTEN_CACHE)
	public String getMaskinportenToken() {
		LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		body.add("assertion", signedJwtClaim());

		return Optional.ofNullable(restClient.post()
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(body)
				.retrieve()
				.body(OidcTokenResponse.class))
				.map(OidcTokenResponse::getAccessToken)
				.orElseThrow(() -> new MaskinportenTechnicalException("Tomt token-svar fra Maskinporten"));
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		if (response.getStatusCode().is4xxClientError()) {
			throw new MaskinportenFunctionalException(FUNKSJONELL_FEIL_ERROR_MESSAGE + errorBody);
		}
		throw new MaskinportenTechnicalException(TEKNISK_FEIL_ERROR_MESSAGE + errorBody);
	}

	private String signedJwtClaim() {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.audience(maskinportenProperties.getIssuer())
				.issuer(dpoProperties.getClientId())
				.claim("scope", dpoProperties.getScope())
				.claim("consumer", Consumer.builder()
						.authority(ISO_6523_ACTORID_UPIS.getValue())
						.id(NAV_ORGNUMMER)
						.build())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
				.expirationTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(30)))
				.build();
		return generateSignedJWTFromCertificate(appCertificate, claims);
	}
}
