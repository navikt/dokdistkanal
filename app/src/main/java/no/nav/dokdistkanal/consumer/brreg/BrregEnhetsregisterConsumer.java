package no.nav.dokdistkanal.consumer.brreg;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterNotFoundException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class BrregEnhetsregisterConsumer {

	private final WebClient webClient;

	public BrregEnhetsregisterConsumer(WebClient webClient,
									   DokdistkanalProperties dokdistkanalProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public HentEnhetResponse hentEnhet(String orgnummer) {
		return webClient.get()
				.uri("/enheter/{orgnummer}", orgnummer)
				.retrieve()
				.bodyToMono(HentEnhetResponse.class)
				.doOnError(handleErrors())
				.block();
	}

	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public EnhetsRolleResponse hentEnhetsRollegrupper(String orgnummer) {
		return webClient.get()
				.uri("/enheter/{orgnummer}/roller", orgnummer)
				.retrieve()
				.bodyToMono(EnhetsRolleResponse.class)
				.doOnError(handleErrors())
				.block();

	}

	private Consumer<Throwable> handleErrors() {
		return error -> {
			if (!(error instanceof WebClientResponseException)) {
				String feilmelding = format("Kall mot Brønnøysundregistrene feilet teknisk med feilmelding=%s", error.getMessage());

				throw new EnhetsregisterFunctionalException(feilmelding, error);
			}

			WebClientResponseException webException = (WebClientResponseException) error;
			String feilmelding = webException.getResponseBodyAsString() == null ? webException.getMessage() : webException.getResponseBodyAsString();
			if (webException.getStatusCode().is4xxClientError()) {
				if (NOT_FOUND.isSameCodeAs(webException.getStatusCode())) {
					throw new EnhetsregisterNotFoundException("Finner ikke organisasjonsnummer i Brønnøysundregistrene med status=" + webException.getStatusCode(), webException);
				}
				throw new EnhetsregisterFunctionalException("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding=" + feilmelding, webException);
			} else {
				throw new EnhetsregisterTechnicalException("Kall mot Brønnøysundregistrene feilet teknisk med feilmelding=" + feilmelding, webException);
			}
		};
	}
}
