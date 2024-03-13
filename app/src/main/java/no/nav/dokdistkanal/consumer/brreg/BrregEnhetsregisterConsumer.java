package no.nav.dokdistkanal.consumer.brreg;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsRegisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsRegisterTechnicalException;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Brønnøysundregistrene
 */
@Component
public class BrregEnhetsregisterConsumer {

	private static final Set<String> ROLLER_TYPE = Set.of("DAGL", "INNH", "LEDE", "BEST", "DTPR", "DTSO");

	public static final String BREG_PATH = "enheter";
	public static final String ROLLER = "roller";
	private final WebClient webClient;

	public BrregEnhetsregisterConsumer(WebClient webClient,
									   DokdistkanalProperties dokdistkanalProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = EnhetsRegisterTechnicalException.class)
	public boolean hentEnhet(String orgNummer) {
		HentEnhetResponse response = webClient.get()
				.uri(uriBuilder -> uriBuilder.pathSegment(BREG_PATH, orgNummer).build())
				.retrieve()
				.bodyToMono(HentEnhetResponse.class)
				.doOnError(handleErrors())
				.block();
		return response == null || response.konkurs();
	}

	@Retryable(retryFor = EnhetsRegisterTechnicalException.class)
	public boolean hentEnhetsRollegrupper(String orgNummer) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.pathSegment(BREG_PATH, orgNummer, ROLLER).build())
				.retrieve()
				.bodyToMono(EnhetsRolleResponse.class)
				.map(this::isContainsValidRolleType)
				.doOnError(handleErrors())
				.block();

	}

	private boolean isContainsValidRolleType(EnhetsRolleResponse response) {
		if (response == null || isEmpty(response.rollegrupper())) {
			return false;
		}

		return response.rollegrupper().stream()
				.flatMap(roller -> roller.roller().stream())
				.filter(Objects::nonNull)
				.filter(rolle -> !erPersonDoedOrIkkeFodselsdato(rolle.person()))
				.anyMatch(r -> ROLLER_TYPE.contains(r.type().kode()));
	}

	private boolean erPersonDoedOrIkkeFodselsdato(EnhetsRolleResponse.Person person) {
		if (person == null) {
			return false;
		}
		return person.erDoed() || person.fodselsdato() == null;
	}

	private Consumer<Throwable> handleErrors() {
		return error -> {
			if (error instanceof WebClientResponseException webException) {
				ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);
				throw new EnhetsRegisterFunctionalException("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding=%s" + problemDetail);
			} else {
				throw new EnhetsRegisterTechnicalException("Kall mot Brønnøysundregistrene feilet teknisk:", error);
			}
		};
	}
}
