package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterNotFoundException;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.PdlTechnicalException;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@Slf4j
@ControllerAdvice(basePackages = "no.nav.dokdistkanal.rest.bestemdistribusjonskanal")
public class BestemDistribusjonskanalErrorHandler extends ResponseEntityExceptionHandler {

	private static final String CONSUMER_FUNKSJONELL_FEIL_MESSAGE = "Funksjonell feil ved kall mot ekstern tjeneste";
	private static final String CONSUMER_TEKNISK_FEIL_MESSAGE = "Teknisk feil ved kall mot ekstern tjeneste";
	private static final String UNAUTHORIZED_FEIL_MESSAGE = "OIDC token mangler eller er ugyldig";
	private static final String UKJENT_TEKNISK_FEIL_MESSAGE = "Ukjent teknisk feil";

	@ExceptionHandler({DokmetFunctionalException.class,
			AltinnServiceOwnerFunctionalException.class,
			DigitalKontaktinformasjonFunctionalException.class,
			EnhetsregisterFunctionalException.class,
			PdlFunctionalException.class
	})
	ProblemDetail handleConsumerFunctionalException(Exception ex) {
		log.warn("{}. Feil={}", CONSUMER_FUNKSJONELL_FEIL_MESSAGE, ex.getMessage(), ex);
		return mapProblemDetail(CONSUMER_FUNKSJONELL_FEIL_MESSAGE, INTERNAL_SERVER_ERROR, ex);
	}

	@ExceptionHandler(EnhetsregisterNotFoundException.class)
	@ResponseStatus(value = NOT_FOUND)
	ProblemDetail handleNotFoundException(Exception ex) {
		log.warn("{}. Feil={}", CONSUMER_FUNKSJONELL_FEIL_MESSAGE, ex.getMessage(), ex);
		return mapProblemDetail(CONSUMER_FUNKSJONELL_FEIL_MESSAGE, NOT_FOUND, ex);
	}

	@ExceptionHandler({DokmetTechnicalException.class,
			AltinnServiceOwnerTechnicalException.class,
			DigitalKontaktinformasjonTechnicalException.class,
			PdlTechnicalException.class,
			PdlTechnicalException.class,
			EnhetsregisterTechnicalException.class
	})
	ProblemDetail handleConsumerTechnicalException(Exception ex) {
		log.error("{}. Feil={}", CONSUMER_TEKNISK_FEIL_MESSAGE, ex.getMessage(), ex);
		return mapProblemDetail(CONSUMER_TEKNISK_FEIL_MESSAGE, INTERNAL_SERVER_ERROR, ex);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String feilmelding = ex.getFieldErrors().stream()
				.map(it -> format("%s, mottatt %s=%s", it.getDefaultMessage(), it.getField(), it.getRejectedValue()))
				.collect(Collectors.joining(". "));

		log.warn("Validering av request feilet med feil={}", feilmelding, ex);

		return ResponseEntity
				.status(BAD_REQUEST)
				.contentType(APPLICATION_PROBLEM_JSON)
				.body(ProblemDetail.forStatusAndDetail(BAD_REQUEST, feilmelding));
	}

	@ExceptionHandler(JwtTokenUnauthorizedException.class)
	ProblemDetail handleJwtTokenException(Exception ex) {
		var feilmelding = UNAUTHORIZED_FEIL_MESSAGE + ". Feil=" + ex.getCause().getMessage();
		log.warn(feilmelding, ex);
		return mapProblemDetail(UNAUTHORIZED_FEIL_MESSAGE, UNAUTHORIZED, ex);
	}

	@ExceptionHandler({CallNotPermittedException.class})
	ProblemDetail handleCallNotPermittedException(Exception ex) {
		log.warn("{}. Feil={}", CONSUMER_TEKNISK_FEIL_MESSAGE, ex.getMessage(), ex);
		return mapProblemDetail(CONSUMER_TEKNISK_FEIL_MESSAGE, SERVICE_UNAVAILABLE, ex);
	}

	@ExceptionHandler({Exception.class})
	ProblemDetail handleException(Exception ex) {
		log.error(ex.getMessage(), ex);

		return mapProblemDetail(UKJENT_TEKNISK_FEIL_MESSAGE, INTERNAL_SERVER_ERROR, ex);
	}

	private ProblemDetail mapProblemDetail(String title, HttpStatusCode httpStatusCode, Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatusCode, ex.getMessage());
		problem.setTitle(title);
		return problem;
	}

}
