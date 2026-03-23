package no.nav.dokdistkanal.consumer.dokmet;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dokmet.map.DokumenttypeInfoMapper;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.DOKMET_CACHE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class DokmetConsumer {

	private final RestClient restClient;

	public DokmetConsumer(DokdistkanalProperties dokdistkanalProperties,
						  RestClient restClientTexas) {
		this.restClient = restClientTexas
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	@Cacheable(value = DOKMET_CACHE)
	@CircuitBreaker(name = "dokmet")
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public DokumentTypeKanalInfo hentDokumenttypeInfo(final String dokumenttypeId) {
		DokumentTypeInfoTo response = restClient.get()
				.uri("/" + dokumenttypeId)
				.retrieve()
				.body(DokumentTypeInfoTo.class);

		return DokumenttypeInfoMapper.mapTo(response);
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot dokmet feilet %s med status=%s, body=%s"
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
						response.getStatusCode(), body);
		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(feilmelding);
		}
		throw new DokmetTechnicalException(feilmelding);
	}
}