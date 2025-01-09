package no.nav.dokdistkanal.consumer.dki;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerRequest;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerResponse;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonTechnicalException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

import static java.lang.String.format;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_DIGDIR_KRR_PROXY;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper.mapDigitalKontaktinformasjon;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer {

	private static final String PERSON_IKKE_FUNNET_FEILKODE = "person_ikke_funnet";
	private static final String SIKKER_DIGITAL_POSTADRESSE_URI = "/rest/v1/personer?inkluderSikkerDigitalPost={inkluderSikkerDigitalPost}";
	private static final String RESILIENCE4J_INSTANCE = "digdir-krr-proxy";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public DigitalKontaktinformasjonConsumer(DokdistkanalProperties dokdistkanalProperties,
											 @Qualifier("azureOauth2WebClient") WebClient webClient,
											 CircuitBreakerRegistry circuitBreakerRegistry,
											 RetryRegistry retryRegistry) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDigdirKrrProxy().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALL_ID))
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {

		final String fnrTrimmed = personidentifikator.trim();

		PostPersonerResponse response = webClient.post()
				.uri(SIKKER_DIGITAL_POSTADRESSE_URI, inkluderSikkerDigitalPost)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DIGDIR_KRR_PROXY))
				.bodyValue(new PostPersonerRequest(List.of(fnrTrimmed)))
				.retrieve()
				.bodyToMono(PostPersonerResponse.class)
				.doOnError(this::handleError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

		if (isValidRespons(response, fnrTrimmed)) {
			return mapDigitalKontaktinformasjon(response.getPersoner().get(fnrTrimmed));
		} else {
			String feilkode = getFeilkode(response, fnrTrimmed);

			if (isPersonIkkeFunnet(feilkode)) {
				return null;
			} else {
				throw new DigitalKontaktinformasjonFunctionalException(format("Kall mot digdir-krr-proxy feilet funksjonelt med feilmelding=%s", feilkode == null ? "Ingen feilmelding" : feilkode));
			}
		}
	}

	private boolean isValidRespons(PostPersonerResponse response, String fnr) {
		return response != null && response.getPersoner() != null && response.getPersoner().get(fnr) != null;
	}

	private String getFeilkode(PostPersonerResponse response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr);
		}
	}

	private static boolean isPersonIkkeFunnet(String feil) {
		return feil != null && feil.contains(PERSON_IKKE_FUNNET_FEILKODE);
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot digdir-krr-proxy feilet teknisk med feilmelding=%s", error.getMessage());
			throw new DigitalKontaktinformasjonTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot digdir-krr-proxy feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		if (response.getStatusCode().is4xxClientError()) {
			throw new DigitalKontaktinformasjonFunctionalException(feilmelding, error);
		} else {
			throw new DigitalKontaktinformasjonTechnicalException(feilmelding, error);
		}
	}

}
