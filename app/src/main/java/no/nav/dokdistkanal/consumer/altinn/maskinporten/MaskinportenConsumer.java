package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersFilter;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.MaskinportenTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static java.util.Date.from;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.constants.DomainConstants.DEFAULT_ZONE_ID;
import static no.nav.dokdistkanal.constants.DomainConstants.NAV_ORGNUMMER;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.asIso6523;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.createSignedJWT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@Slf4j
@Component
public class MaskinportenConsumer {

	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final WebClient webClient;
	private final MaskinportenProperties maskinportenProperties;

	@Autowired
	public MaskinportenConsumer(WebClient webClient,
								MaskinportenProperties maskinportenProperties) {
		this.maskinportenProperties = maskinportenProperties;
		this.webClient = webClient.mutate()
				.baseUrl(maskinportenProperties.getTokenEndpoint())
				.defaultHeader(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
				.filter(new NavHeadersFilter(CALL_ID))
				.build();
	}

	@Cacheable(MASKINPORTEN_CACHE)
	public String getMaskinportenToken() {

		LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
		attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		attrMap.add("assertion", generateJWT());

		OidcTokenResponse tokenResponse = webClient.post()
				.uri(maskinportenProperties.getTokenEndpoint())
				.bodyValue(attrMap)
				.retrieve()
				.bodyToMono(OidcTokenResponse.class)
				.doOnError(err -> {
					if (err instanceof WebClientResponseException respose &&
							((WebClientResponseException) err).getStatusCode().is4xxClientError()) {
						final String errorMessage = FUNKSJONELL_FEIL_ERROR_MESSAGE + respose.getResponseBodyAsString();
						log.warn(errorMessage, errorMessage);
						throw new MaskinportenFunctionalException(errorMessage, err);
					}
					final String errorMessage = TEKNISK_FEIL_ERROR_MESSAGE + err.getMessage();
					log.error(errorMessage, err);
					throw new MaskinportenTechnicalException(errorMessage, err);
				})
				.block();

		return tokenResponse.getAccessToken();
	}

	@SneakyThrows
	private String generateJWT() {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.audience(maskinportenProperties.getIssuer())
				.issuer(maskinportenProperties.getClientId())
				.claim("scope", getCurrentScopes())
				.claim("consumer", Consumer.builder()
						.authority(ISO_6523_ACTORID_UPIS.getValue())
						.id(asIso6523(NAV_ORGNUMMER))
						.build())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
				.expirationTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(30)))
				.build();
		var rsaKey = RSAKey.parse(maskinportenProperties.getClientJwk());

		return createSignedJWT(rsaKey, claims)
				.serialize();
	}

	private String getCurrentScopes() {
		ArrayList<String> scopeList = new ArrayList<>();
		scopeList.add(maskinportenProperties.getScopes());
		return scopeList.stream().reduce((a, b) -> a + " " + b).orElse("");
	}
}
