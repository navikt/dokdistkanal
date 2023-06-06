package no.nav.dokdistkanal.consumer.altinn.serviceowner;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.slf4j.MDC;
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

import static no.nav.dokdistkanal.common.FunctionalUtils.createHttpClient;
import static no.nav.dokdistkanal.common.FunctionalUtils.getOrCreateCallId;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpMethod.GET;

@Component
public class AltinnServiceOwnerConsumer {

	private static final String SERVICEOWNER_PATH = "/serviceowner/notifications/validaterecipient";

	private final MaskinportenConsumer maskinportenConsumer;
	private final DokdistkanalProperties dokdistkanalProperties;
	private final RestTemplate restTemplate;

	public AltinnServiceOwnerConsumer(DokdistkanalProperties dokdistkanalProperties,
									  RestTemplateBuilder restTemplateBuilder,
									  HttpClientConnectionManager httpClientConnectionManager,
									  MaskinportenConsumer maskinportenConsumer) {
		this.maskinportenConsumer = maskinportenConsumer;
		this.dokdistkanalProperties = dokdistkanalProperties;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(createHttpClient(dokdistkanalProperties.getProxy(), httpClientConnectionManager)))
				.build();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class, backoff = @Backoff(delay = 200))
	public ValidateRecipientResponse isServiceOwnerValidReciepient(String orgNummer) {
		String altinnUrl = UriComponentsBuilder.fromUriString(dokdistkanalProperties.getAltinn().getUrl())
				.path(SERVICEOWNER_PATH)
				.queryParam("organizationNumber", orgNummer)
				.queryParam("serviceCode", dokdistkanalProperties.getAltinn().getServiceCode())
				.queryParam("serviceEditionCode", dokdistkanalProperties.getAltinn().getServiceEditionCode())
				.build().toString();

		HttpEntity httpEntity = new HttpEntity<>(headers());

		try {
			ResponseEntity<ValidateRecipientResponse> response = restTemplate.exchange(altinnUrl, GET, httpEntity, ValidateRecipientResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException err) {
			throw new AltinnServiceOwnerFunctionalException(err.getMessage(), err);
		} catch (HttpServerErrorException err) {
			throw new AltinnServiceOwnerTechnicalException(err.getMessage(), err);
		}
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(ACCEPT, HAL_JSON_VALUE);
		headers.setBearerAuth(maskinportenConsumer.getMaskinportenToken());
		headers.set("ApiKey", dokdistkanalProperties.getAltinn().getApiKey());
		headers.set(NAV_CALL_ID, getOrCreateCallId(MDC.get(CALL_ID)));
		return headers;
	}
}
