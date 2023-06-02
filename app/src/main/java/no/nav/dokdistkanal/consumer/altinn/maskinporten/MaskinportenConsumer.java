package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.MaskinportenTechnicalException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import static no.nav.dokdistkanal.common.FunctionalUtils.getOrCreateCallId;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.constants.DomainConstants.DEFAULT_ZONE_ID;
import static no.nav.dokdistkanal.constants.DomainConstants.NAV_ORGNUMMER;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.asIso6523;
import static no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenUtils.createSignedJWT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@Component
public class MaskinportenConsumer {

	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final RestTemplate restTemplate;
	private final MaskinportenProperties maskinportenProperties;

	@Autowired
	public MaskinportenConsumer(RestTemplateBuilder restTemplateBuilder,
								MaskinportenProperties maskinportenProperties) {
		this.maskinportenProperties = maskinportenProperties;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Cacheable(MASKINPORTEN_CACHE)
	public String getMaskinportenToken() {

		LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
		attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		attrMap.add("assertion", generateJWT());

		HttpEntity httpEntity = new HttpEntity<>(attrMap, headers());

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

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		headers.set(NAV_CALL_ID, getOrCreateCallId(MDC.get(CALL_ID)));
		return headers;
	}
}
