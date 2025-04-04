package no.nav.dokdistkanal.consumer.altinn.serviceowner;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

import static no.nav.dokdistkanal.constants.DomainConstants.HAL_JSON_VALUE;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Component
public class AltinnServiceOwnerConsumer {

	private static final String SERVICEOWNER_PATH = "/serviceowner/notifications/validaterecipient";
	private static final String ALTINN_API_KEY = "ApiKey";

	private final MaskinportenConsumer maskinportenConsumer;
	private final DokdistkanalProperties dokdistkanalProperties;
	private final RestTemplate restTemplate;

	public AltinnServiceOwnerConsumer(DokdistkanalProperties dokdistkanalProperties,
									  RestTemplateBuilder restTemplateBuilder,
									  MaskinportenConsumer maskinportenConsumer,
									  HttpClient httpClient) {
		this.maskinportenConsumer = maskinportenConsumer;
		this.dokdistkanalProperties = dokdistkanalProperties;
		this.restTemplate = restTemplateBuilder
				.connectTimeout(Duration.ofSeconds(3L))
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
				.build();
	}

	@Retryable(retryFor = DokdistkanalTechnicalException.class, backoff = @Backoff(delay = 200))
	public ValidateRecipientResponse isServiceOwnerValidRecipient(String organisasjonsnummer) {
		String altinnUrl = UriComponentsBuilder.fromUriString(dokdistkanalProperties.getAltinn().getUrl())
				.path(SERVICEOWNER_PATH)
				.queryParam("organizationNumber", organisasjonsnummer)
				.build().toString();

		var httpEntity = new HttpEntity<>(headers());

		try {
			ResponseEntity<ValidateRecipientResponse> response = restTemplate.exchange(altinnUrl, GET, httpEntity, ValidateRecipientResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException err) {
			throw new AltinnServiceOwnerFunctionalException(err.getMessage(), err);
		} catch (HttpServerErrorException err) {
			if (FORBIDDEN == err.getStatusCode()) {
				throw new AltinnServiceOwnerFunctionalException(err.getMessage(), err);
			}
			throw new AltinnServiceOwnerTechnicalException(err.getMessage(), err);
		}
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(ACCEPT, HAL_JSON_VALUE);
		headers.setBearerAuth(maskinportenConsumer.getMaskinportenToken());
		headers.set(ALTINN_API_KEY, dokdistkanalProperties.getAltinn().getApiKey());
		return headers;
	}
}
