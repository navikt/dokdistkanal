package no.nav.dokdistkanal.consumer.pdl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.nais.NaisTexasRequestInterceptor;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.PdlTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";

	private final RestClient restClient;
	private final String targetScope;

	public PdlGraphQLConsumer(DokdistkanalProperties dokdistkanalProperties,
							  RestClient restClientTexas) {
		this.restClient = restClientTexas
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getPdl().getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
					httpHeaders.set(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
				})
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
		this.targetScope = dokdistkanalProperties.getEndpoints().getPdl().getScope();
	}

	@CircuitBreaker(name = "pdl")
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public HentPersoninfo hentPerson(final String aktoerId) {

		log.debug("Henter personinfo for aktørId={}", aktoerId);

		PDLHentPersonResponse response = restClient.post()
				.attribute(NaisTexasRequestInterceptor.TARGET_SCOPE, targetScope)
				.body(mapRequest(aktoerId))
				.retrieve()
				.body(PDLHentPersonResponse.class);

		return mapPersonInfo(response);
	}

	private HentPersoninfo mapPersonInfo(PDLHentPersonResponse response) {
		if (response == null) {
			return null;
		}

		if (response.getErrors() != null) {
			log.info("Kunne ikke hente person fra Pdl. Response inneholdt feilmeldinger: {}",
					response.getErrors().stream().map(PDLHentPersonResponse.PdlError::getMessage)
							.collect(Collectors.joining(",")));
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
					.foedselsdato(hentPerson.getFoedselsdato() == null ? null : hentPerson.getFoedselsdato().stream()
							.map(PDLHentPersonResponse.Foedselsdato::getFoedselsdato)
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
				    foedselsdato {
				      foedselsdato
				    }
				  }
				}""").variables(variables).build();
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot pdl feilet %s med status=%s, body=%s"
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
						response.getStatusCode(), body);
		log.warn(feilmelding);
		if (response.getStatusCode().is4xxClientError()) {
			throw new PdlFunctionalException(feilmelding);
		}
		throw new PdlTechnicalException(feilmelding, null);
	}
}
