package no.nav.dokdistkanal.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dokmet.map.DokumenttypeInfoMapper;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoToV4;
import no.nav.dokdistkanal.exceptions.functional.DokdistkanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_DOKMET;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.HENT_DOKUMENTTYPE_INFO_CACHE;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Service
@Slf4j
public class DokumentTypeInfoConsumer {

	private static final String DOKUMENTTYPE_INFO_URI = "/rest/dokumenttypeinfo/{dokumenttypeId}";

	private final WebClient webClient;

	public DokumentTypeInfoConsumer(DokdistkanalProperties dokdistkanalProperties,
									@Qualifier("azureOauth2WebClient") WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALLID))
				.build();
	}

	@Cacheable(value = HENT_DOKUMENTTYPE_INFO_CACHE)
	@Retryable(retryFor = DokdistkanalTechnicalException.class, noRetryFor = DokdistkanalFunctionalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) {

		return webClient.get()
				.uri(DOKUMENTTYPE_INFO_URI, dokumenttypeId)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKMET))
				.retrieve()
				.bodyToMono(DokumentTypeInfoToV4.class)
				.mapNotNull(DokumenttypeInfoMapper::mapTo)
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new DokmetTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot dokmet feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(feilmelding, error);
		} else {
			throw new DokmetTechnicalException(feilmelding, error);
		}
	}

}