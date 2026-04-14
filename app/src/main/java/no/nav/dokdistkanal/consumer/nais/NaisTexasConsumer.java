package no.nav.dokdistkanal.consumer.nais;

import no.nav.dokdistkanal.config.nais.NaisProperties;
import no.nav.dokdistkanal.exceptions.functional.NaisTexasFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.NaisTexasTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static java.lang.String.join;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Component
public class NaisTexasConsumer {

	private final RestClient restClient;

	public NaisTexasConsumer(RestClient.Builder restClientBuilder, NaisProperties naisProperties) {
		this.restClient = restClientBuilder
				.baseUrl(naisProperties.tokenEndpoint())
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	public String getSystemToken(String targetScope) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "entra_id");
		formData.add("target", targetScope);

		return Optional.ofNullable(restClient.post()
				.contentType(APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class))
				.map(NaisTexasToken::accessToken)
				.orElseThrow(() -> new NaisTexasTechnicalException("Tomt token-svar fra NAIS Texas (entra_id)"));
	}

	public String getMaskinportenToken(String... targetScopes) {
		String formattedScopes = join(" ", targetScopes);

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "maskinporten");
		formData.add("target", formattedScopes);

		return Optional.ofNullable(restClient.post()
				.contentType(APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class))
				.map(NaisTexasToken::accessToken)
				.orElseThrow(() -> new NaisTexasTechnicalException("Tomt token-svar fra NAIS Texas (maskinporten)"));
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Kall mot nais-texas feilet %s med status=%s, body=%s"
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
						response.getStatusCode(), body);
		if (response.getStatusCode().is4xxClientError()) {
			throw new NaisTexasFunctionalException(feilmelding);
		}
		throw new NaisTexasTechnicalException(feilmelding);
	}
}
