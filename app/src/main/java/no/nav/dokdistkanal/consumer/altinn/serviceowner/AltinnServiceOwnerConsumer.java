package no.nav.dokdistkanal.consumer.altinn.serviceowner;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static no.nav.dokdistkanal.constants.DomainConstants.HAL_JSON_VALUE;
import static no.nav.dokdistkanal.consumer.nais.NaisTexasRequestInterceptor.MASKINPORTEN_SCOPE;
import static org.springframework.http.HttpHeaders.ACCEPT;

@Component
public class AltinnServiceOwnerConsumer {

	private static final String SERVICEOWNER_PATH = "/serviceowner/notifications/validaterecipient";
	private static final String ALTINN_API_KEY = "ApiKey";

	private final RestClient restClient;
	private final String maskinportenScope;

	public AltinnServiceOwnerConsumer(DokdistkanalProperties dokdistkanalProperties,
									  RestClient restClientTexas) {
		this.restClient = restClientTexas.mutate()
				.baseUrl(dokdistkanalProperties.getAltinn().getUrl())
				.defaultHeader(ACCEPT, HAL_JSON_VALUE)
				.defaultHeader(ALTINN_API_KEY, dokdistkanalProperties.getAltinn().getApiKey())
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
		this.maskinportenScope = dokdistkanalProperties.getAltinn().getMaskinportenScope();
	}

	@Retryable(includes = DokdistkanalTechnicalException.class, delay = 200)
	public ValidateRecipientResponse isServiceOwnerValidRecipient(String organisasjonsnummer) {
		return restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(SERVICEOWNER_PATH)
						.queryParam("organizationNumber", organisasjonsnummer)
						.build())
				.attribute(MASKINPORTEN_SCOPE, maskinportenScope)
				.retrieve()
				.body(ValidateRecipientResponse.class);
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot altinn feilet %s med status=%s, body=%s"
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
						response.getStatusCode(), body);
		if (response.getStatusCode().is4xxClientError()) {
			throw new AltinnServiceOwnerFunctionalException(feilmelding);
		}
		throw new AltinnServiceOwnerTechnicalException(feilmelding);
	}
}
