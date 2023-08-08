package no.nav.dokdistkanal.consumer.dki;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.DkifResponseTo;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerRequest;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_DIGDIR_KRR_PROXY;
import static no.nav.dokdistkanal.azure.AzureProperties.getOAuth2AuthorizeRequestForAzure;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper.mapDigitalKontaktinformasjon;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer implements DigitalKontaktinformasjon {

	private static final String INGEN_KONTAKTINFORMASJON_FEILMELDING = "person_ikke_funnet";
	private static final String SIKKER_DIGITAL_POSTADRESSE_URI = "/rest/v1/personer?inkluderSikkerDigitalPost={inkluderSikkerDigitalPost}";

	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DigitalKontaktinformasjonConsumer(DokdistkanalProperties dokdistkanalProperties,
											 ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
											 WebClient webClient) {
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDigdirKrrProxy().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALLID))
				.build();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class, noRetryFor = DokDistKanalFunctionalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {

		final String fnrTrimmed = personidentifikator.trim();

		DkifResponseTo response = webClient.post()
				.uri(SIKKER_DIGITAL_POSTADRESSE_URI, inkluderSikkerDigitalPost)
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(new PostPersonerRequest(List.of(fnrTrimmed)))
				.retrieve()
				.bodyToMono(DkifResponseTo.class)
				.doOnError(this::handleError)
				.block();

		if (isValidRespons(response, fnrTrimmed)) {
			return mapDigitalKontaktinformasjon(response.getPersoner().get(fnrTrimmed));
		} else {
			String errorMsg = getErrorMsg(response, fnrTrimmed);

			if (errorMsg != null && errorMsg.contains(INGEN_KONTAKTINFORMASJON_FEILMELDING)) {
				return null;
			} else {
				throw new DigitalKontaktinformasjonV2FunctionalException(format("Kall mot digdir-krr-proxy feilet funksjonelt med feilmelding=%s",
						errorMsg == null ? "Ingen feilmelding" : errorMsg));
			}
		}
	}

	private boolean isValidRespons(DkifResponseTo response, String fnr) {
		return response != null && response.getPersoner() != null && response.getPersoner().get(fnr) != null;
	}

	private String getErrorMsg(DkifResponseTo response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr);
		}
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot digdir-krr-proxy feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new DigitalKontaktinformasjonV2TechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot digdir-krr-proxy feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DigitalKontaktinformasjonV2FunctionalException(feilmelding, error);
		} else {
			throw new DigitalKontaktinformasjonV2TechnicalException(feilmelding, error);
		}
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_DIGDIR_KRR_PROXY));
		return oauth2AuthorizedClient(clientMono.block());
	}

}
