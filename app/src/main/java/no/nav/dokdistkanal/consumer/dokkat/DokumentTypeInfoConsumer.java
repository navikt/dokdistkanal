package no.nav.dokdistkanal.consumer.dokkat;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoToV4;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokkatTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_DOKMET;
import static no.nav.dokdistkanal.azure.AzureProperties.getOAuth2AuthorizeRequestForAzure;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.HENT_DOKUMENTTYPE_INFO_CACHE;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
@Slf4j
public class DokumentTypeInfoConsumer {

	private static final String DOKUMENTTYPE_INFO_URI = "/rest/dokumenttypeinfo/{dokumenttypeId}";

	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DokumentTypeInfoConsumer(DokdistkanalProperties dokdistkanalProperties,
									WebClient webClient,
									ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALLID))
				.build();

	}

	@Cacheable(value = HENT_DOKUMENTTYPE_INFO_CACHE)
	@Retryable(retryFor = DokDistKanalTechnicalException.class, noRetryFor = DokDistKanalFunctionalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) {

		return webClient.get()
				.uri(DOKUMENTTYPE_INFO_URI, dokumenttypeId)
				.attributes(getOAuth2AuthorizedClient())
				.retrieve()
				.bodyToMono(DokumentTypeInfoToV4.class)
				.mapNotNull(this::mapTo)
				.doOnError(this::handleError)
				.block();
	}

	private DokumentTypeInfoTo mapTo(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		String predefinertDistribusjonKanal = null;
		if (!(dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
				.getDistribusjonInfo() == null)) {
			predefinertDistribusjonKanal = dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
					.getDistribusjonInfo()
					.getPredefinertDistKanal();
		}

		if (dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
				.getDistribusjonInfo() == null
				|| dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null) {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(FALSE).build();
		} else {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(
									distribusjonVarselTo -> SDP.toString()
											.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new DokkatTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot dokmet feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokkatFunctionalException(feilmelding, error);
		} else {
			throw new DokkatTechnicalException(feilmelding, error);
		}
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_DOKMET));
		return oauth2AuthorizedClient(clientMono.block());
	}

}