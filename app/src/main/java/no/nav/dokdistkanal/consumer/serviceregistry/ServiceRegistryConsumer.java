package no.nav.dokdistkanal.consumer.serviceregistry;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.ServiceRegistryTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ServiceRegistryConsumer {

	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Teknisk feil: ";

	private final RestClient restClient;
	private final MaskinportenConsumer maskinportenConsumer;

	public ServiceRegistryConsumer(RestClient restClientTexas,
								   DokdistkanalProperties dokdistkanalProperties,
								   MaskinportenConsumer maskinportenConsumer) {
		this.restClient = restClientTexas.mutate()
				.baseUrl(dokdistkanalProperties.getServiceRegistry().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
		this.maskinportenConsumer = maskinportenConsumer;
	}

	@Retryable(includes = DokdistkanalTechnicalException.class)
	public IdentifierResource getIdentifierResource(final String orgnummer, final String processIdentifier) {
		return restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/identifier/{orgnummer}/process/" + processIdentifier)
						.build(orgnummer))
				.headers(httpHeaders -> httpHeaders.setBearerAuth(maskinportenConsumer.getMaskinportenToken()))
				.exchange((req, res) -> {
					if (res.getStatusCode().isError()) {
						if (res.getStatusCode().is4xxClientError()) {
							return null;
						}
						handleError(res);
					}
					return res.bodyTo(IdentifierResource.class);
				});
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot service-registry feilet teknisk med status=%s, body=%s"
				.formatted(response.getStatusCode(), body);
		log.error(feilmelding);
		throw new ServiceRegistryTechnicalException(TEKNISK_FEIL_ERROR_MESSAGE + body);
	}
}
