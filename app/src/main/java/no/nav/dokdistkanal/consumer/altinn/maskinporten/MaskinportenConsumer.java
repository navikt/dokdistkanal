package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.MaskinportenTechnicalException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static java.util.Date.from;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.constants.DomainConstants.DEFAULT_ZONE_ID;
import static no.nav.dokdistkanal.constants.DomainConstants.NAV_ORGNUMMER;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.createSignedJWT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@Component
public class MaskinportenConsumer {

	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final RestTemplate restTemplate;
	private final MaskinportenProperties maskinportenProperties;

	public MaskinportenConsumer(RestTemplateBuilder restTemplateBuilder,
								MaskinportenProperties maskinportenProperties,
								HttpClient httpClient) {
		this.maskinportenProperties = maskinportenProperties;
		this.restTemplate = restTemplateBuilder
				.connectTimeout(Duration.ofSeconds(3L))
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
				.build();
	}

	@Cacheable(MASKINPORTEN_CACHE)
	public String getMaskinportenToken() {

		LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
		attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		attrMap.add("assertion", generateJWT());

		HttpEntity<LinkedMultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers());

		try {
			ResponseEntity<OidcTokenResponse> tokenResponse = restTemplate.exchange(maskinportenProperties.getTokenEndpoint(), HttpMethod.POST, httpEntity, OidcTokenResponse.class);
			return tokenResponse.getBody().getAccessToken();
		} catch (HttpClientErrorException err) {
			final String errorMessage = FUNKSJONELL_FEIL_ERROR_MESSAGE + err.getResponseBodyAsString();
			log.warn(errorMessage, errorMessage);
			throw new MaskinportenFunctionalException(errorMessage, err);
		} catch (HttpServerErrorException err) {
			final String errorMessage = TEKNISK_FEIL_ERROR_MESSAGE + err.getMessage();
			log.error(errorMessage, err);
			throw new MaskinportenTechnicalException(errorMessage, err);
		}
	}

	private String generateJWT() {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.audience(maskinportenProperties.getIssuer())
				.issuer(maskinportenProperties.getClientId())
				.claim("scope", getCurrentScopes())
				.claim("consumer", Consumer.builder()
						.authority(ISO_6523_ACTORID_UPIS.getValue())
						.id(NAV_ORGNUMMER)
						.build())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
				.expirationTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(30)))
				.build();

		return createSignedJWT(maskinportenProperties.getClientJwk(), claims)
				.serialize();
	}

	private String getCurrentScopes() {
		ArrayList<String> scopeList = new ArrayList<>();
		scopeList.add(maskinportenProperties.getScopes());
		return scopeList.stream().reduce((a, b) -> a + " " + b).orElse("");
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		return headers;
	}
}
