package no.nav.dokdistkanal.consumer.brreg;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import org.jetbrains.annotations.NotNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
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
	public HentEnhetResponse hentHovedenhet(String orgnr) {
		return webClient.get()
				.uri("/enheter/{orgnummer}", orgnr)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(clientResponse.statusCode())) {
							log.warn("organisasjonsnummer={} finner ikke under hovedenheter", orgnr);
							return Mono.empty();
						}
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(HentEnhetResponse.class);
				})
				.block();
	}


	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public EnhetsRolleResponse hentEnhetsRollegrupper(String orgnummer) {
		return webClient.get()
				.uri("/enheter/{orgnummer}/roller", orgnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(EnhetsRolleResponse.class);
				})
				.block();

	}

	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public HentEnhetResponse hentHovedenhetFraUnderenhet(String orgnr) {
		HentUnderenhetResponse hentUnderenhetResponse = webClient.get()
				.uri("/underenheter/{orgnr}", orgnr)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(clientResponse.statusCode())) {
							log.warn("organisasjonsnummer={} verken funnet i hoved eller underenheter", orgnr);
							return Mono.empty();
						}
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(HentUnderenhetResponse.class);
				})
				.block();

		return isNull(hentUnderenhetResponse) ? null : hentHovedenhet(hentUnderenhetResponse.overordnetEnhet());
	}

	@NotNull
	private static <T> Mono<T> handleErrorResponse(ClientResponse clientResponse) {
		if (clientResponse.statusCode().is4xxClientError()) {
			return Mono.error(new EnhetsregisterFunctionalException("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding=" + clientResponse.createError()));
		} else {
			return Mono.error(new EnhetsregisterTechnicalException("Kall mot Brønnøysundregistrene feilet teknisk med feilmelding=" + clientResponse.createError()));
		}
	}
}
