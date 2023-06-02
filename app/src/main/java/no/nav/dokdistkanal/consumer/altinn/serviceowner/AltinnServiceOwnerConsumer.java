package no.nav.dokdistkanal.consumer.altinn.serviceowner;

import no.nav.dokdistkanal.common.NavHeadersFilter;
import no.nav.dokdistkanal.config.WebClientAuthentication;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpHeaders.ACCEPT;

@Component
public class AltinnServiceOwnerConsumer {

	private static final String SERVICEOWNER_PATH = "/serviceowner/notifications/validaterecipient";

	private final WebClient webClient;
	private final DokdistkanalProperties dokdistkanalProperties;

	public AltinnServiceOwnerConsumer(DokdistkanalProperties dokdistkanalProperties,
									  WebClient webClient,
									  MaskinportenConsumer maskinportenConsumer) {
		this.dokdistkanalProperties = dokdistkanalProperties;
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getAltinn().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.set(ACCEPT, HAL_JSON_VALUE))
				.filter(new NavHeadersFilter(CALL_ID))
				.filter(new WebClientAuthentication(maskinportenConsumer.getMaskinportenToken()))
				.build();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class, backoff = @Backoff(delay = 200))
	public ValidateRecipientResponse isServiceOwnerValidReciepient(String orgNummer) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.path(SERVICEOWNER_PATH)
						.queryParam("organizationNumber", orgNummer)
						.queryParam("serviceCode", dokdistkanalProperties.getAltinn().getServiceCode())
						.queryParam("serviceEditionCode", dokdistkanalProperties.getAltinn().getServiceEditionCode())
						.build())
				.header("ApiKey", dokdistkanalProperties.getAltinn().getApiKey())
				.retrieve()
				.bodyToMono(ValidateRecipientResponse.class)
				.doOnError(err -> {
					if (err instanceof WebClientResponseException response && ((WebClientResponseException) err).getStatusCode().is4xxClientError()) {
						throw new AltinnServiceOwnerFunctionalException(err.getMessage(), err);
					}
					throw new AltinnServiceOwnerTechnicalException(err.getMessage(), err);
				})
				.block();
	}
}
