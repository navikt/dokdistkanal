package no.nav.dokdistkanal.consumer.dki;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.nais.NaisTexasRequestInterceptor;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerRequest;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerResponse;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper.mapDigitalKontaktinformasjon;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer {

	private static final String PERSON_IKKE_FUNNET_FEILKODE = "person_ikke_funnet";
	private static final String SIKKER_DIGITAL_POSTADRESSE_URI = "/rest/v1/personer?inkluderSikkerDigitalPost={inkluderSikkerDigitalPost}";

	private final RestClient restClient;
	private final String targetScope;

	public DigitalKontaktinformasjonConsumer(DokdistkanalProperties dokdistkanalProperties,
											 RestClient restClientTexas) {
		this.restClient = restClientTexas
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDigdirKrrProxy().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
		this.targetScope = dokdistkanalProperties.getEndpoints().getDigdirKrrProxy().getScope();
	}

	@CircuitBreaker(name = "digdir-krr-proxy")
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {

		final String fnrTrimmed = personidentifikator.trim();

		PostPersonerResponse response = restClient.post()
				.uri(SIKKER_DIGITAL_POSTADRESSE_URI, inkluderSikkerDigitalPost)
				.attribute(NaisTexasRequestInterceptor.TARGET_SCOPE, targetScope)
				.body(new PostPersonerRequest(List.of(fnrTrimmed)))
				.retrieve()
				.body(PostPersonerResponse.class);

		if (isValidRespons(response, fnrTrimmed)) {
			return mapDigitalKontaktinformasjon(response.getPersoner().get(fnrTrimmed));
		} else {
			String feilkode = getFeilkode(response, fnrTrimmed);

			if (isPersonIkkeFunnet(feilkode)) {
				return null;
			} else {
				throw new DigitalKontaktinformasjonFunctionalException("Kall mot digdir-krr-proxy feilet funksjonelt med feilmelding=%s".formatted(feilkode == null ? "Ingen feilmelding" : feilkode));
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

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot digdir-krr-proxy feilet %s med status=%s, body=%s"
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
						response.getStatusCode(), body);
		log.warn(feilmelding);
		if (response.getStatusCode().is4xxClientError()) {
			throw new DigitalKontaktinformasjonFunctionalException(feilmelding);
		}
		throw new DigitalKontaktinformasjonTechnicalException(feilmelding, null);
	}
}
