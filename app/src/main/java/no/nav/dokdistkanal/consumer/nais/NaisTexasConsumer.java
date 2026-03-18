package no.nav.dokdistkanal.consumer.nais;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.nais.NaisProperties;
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

@Slf4j
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
		formData.add("identity_provider", "azuread");
		formData.add("target", targetScope);

		return Optional.ofNullable(restClient.post()
				.contentType(APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class))
				.map(NaisTexasToken::accessToken)
				.orElseThrow(() -> new RuntimeException("Tomt token-svar fra NAIS Texas (azuread)"));
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
				.orElseThrow(() -> new RuntimeException("Tomt token-svar fra NAIS Texas (maskinporten)"));
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		String feilmelding = "Tokenforespørsel til NAIS Texas feilet med status=%s, body=%s"
				.formatted(response.getStatusCode(), body);
		log.error(feilmelding);
		throw new RuntimeException(feilmelding);
	}
}
