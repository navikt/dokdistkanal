package no.nav.dokdistkanal.consumer.brreg;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsRegisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsRegisterTechnicalException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Brønnøysundregistrene
 */
@Component
public class BrregEnhetsregisterConsumer {

	private static final String BREG_PATH = "enheter";
	private static final String ROLLER = "roller";
	private final WebClient webClient;

	public BrregEnhetsregisterConsumer(WebClient webClient,
									   DokdistkanalProperties dokdistkanalProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class)
	public HentEnhetResponse hentEnhet(String orgNummer) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.pathSegment(BREG_PATH, orgNummer).build())
				.retrieve()
				.bodyToMono(HentEnhetResponse.class)
				.doOnError(handleErrors())
				.block();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class)
	public EnhetsRolleResponse hentEnhetsRollegrupper(String orgNummer) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.pathSegment(BREG_PATH, orgNummer, ROLLER).build())
				.retrieve()
				.bodyToMono(EnhetsRolleResponse.class)
				.doOnError(handleErrors())
				.block();

	}

	private Consumer<Throwable> handleErrors() {
		return error -> {
			if (error instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
				throw new EnhetsRegisterFunctionalException("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding=" + webException.getMessage(), webException);
			} else {
				throw new EnhetsRegisterTechnicalException("Kall mot Brønnøysundregistrene feilet teknisk:", error);
			}
		};
	}
}
