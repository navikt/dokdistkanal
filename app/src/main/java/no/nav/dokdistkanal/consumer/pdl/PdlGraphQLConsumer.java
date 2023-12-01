package no.nav.dokdistkanal.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.PdlTechnicalException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_PDL;
import static no.nav.dokdistkanal.azure.AzureProperties.getOAuth2AuthorizeRequestForAzure;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	// https://pdldocs-navno.msappproxy.net/ekstern/index.html#_dokumenter_hjemmel
	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";
	// https://behandlingskatalog.nais.adeo.no/process/purpose/ARKIVPLEIE/756fd557-b95e-4b20-9de9-6179fb8317e6
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";
	private static final String HEADER_PDL_TEMA = "Tema";

	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public PdlGraphQLConsumer(DokdistkanalProperties dokdistkanalProperties,
							  WebClient webClient,
							  ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getPdl().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALL_ID))
				.build();
	}

	@Retryable(retryFor = PdlTechnicalException.class)
	public HentPersoninfo hentPerson(final String aktoerId, final String tema) {

		log.debug("Henter personinfo for aktørId={}", aktoerId);

		return webClient.post()
				.attributes(getOAuth2AuthorizedClient())
				.header(HEADER_PDL_TEMA, tema)
				.header(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER)
				.bodyValue(mapRequest(aktoerId))
				.retrieve()
				.bodyToMono(PDLHentPersonResponse.class)
				.mapNotNull(this::mapPersonInfo)
				.doOnError(this::handleError)
				.block();
	}

	private HentPersoninfo mapPersonInfo(PDLHentPersonResponse response) {
		if (response.getErrors() != null ){
			return null;
		}

		if (response.getData() == null || response.getData().getHentPerson() == null) {
			throw new PdlFunctionalException("Kunne ikke hente person fra Pdl. Response inneholdt ingen feilmeldinger, men heller ingen data om etterspurt person.");
		} else {
			PDLHentPersonResponse.HentPerson hentPerson = response.getData().getHentPerson();
			return HentPersoninfo.builder()
					.doedsdato(hentPerson.getDoedsfall() == null ? null : hentPerson.getDoedsfall().stream()
							.map(PDLHentPersonResponse.Doedsfall::getDoedsdato)
							.filter(Objects::nonNull)
							.findAny().orElse(null))
					.foedselsdato(hentPerson.getFoedsel() == null ? null : hentPerson.getFoedsel().stream()
							.map(PDLHentPersonResponse.Foedsel::getFoedselsdato)
							.filter(Objects::nonNull)
							.findAny().orElse(null))
					.build();
		}
	}

	private PDLRequest mapRequest(final String aktoerId) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", aktoerId);

		return PDLRequest.builder().query("""
				query hentPerson($ident: ID!) {
				  hentPerson(ident: $ident) {
				     doedsfall {
				        doedsdato
				     }
				     foedsel {
				        foedselsdato
				     }
				  }
				}""").variables(variables).build();
	}


	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot pdl feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new PdlTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot pdl feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new PdlFunctionalException(feilmelding, error);
		} else {
			throw new PdlTechnicalException(feilmelding, error);
		}
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_PDL));
		return oauth2AuthorizedClient(clientMono.block());
	}

}
