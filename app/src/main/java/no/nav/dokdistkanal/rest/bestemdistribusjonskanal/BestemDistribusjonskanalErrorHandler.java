package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.PdlTechnicalException;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@Slf4j
@ControllerAdvice(basePackages = "no.nav.dokdistkanal.rest.bestemdistribusjonskanal")
public class BestemDistribusjonskanalErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({DokmetFunctionalException.class,
			AltinnServiceOwnerFunctionalException.class,
			DigitalKontaktinformasjonV2FunctionalException.class,
			PdlFunctionalException.class
	})
	ProblemDetail handleConsumerFunctionalException(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
		problem.setTitle("Funksjonell feil ved kall mot ekstern tjeneste");

		log.warn("Funksjonell feil ved kall mot ekstern tjeneste. Feilmelding={}", problem.getDetail(), ex);

		return problem;
	}

	@ExceptionHandler({DokmetTechnicalException.class,
			AltinnServiceOwnerTechnicalException.class,
			DigitalKontaktinformasjonV2TechnicalException.class,
			PdlTechnicalException.class
	})
	ProblemDetail handleConsumerTechnicalException(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
		problem.setTitle("Teknisk feil ved kall mot ekstern tjeneste");

		log.warn("Teknisk feil ved kall mot ekstern tjeneste. Feilmelding={}", problem.getDetail(), ex);

		return problem;
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
		var feilmelding = format("Ugyldig OIDC token mangler eller er ugyldig. Feil=%s", ex.getCause().getMessage());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(UNAUTHORIZED, feilmelding);
		problem.setTitle("OIDC token mangler eller er ugyldig");

		log.warn(feilmelding, ex);

		return problem;
	}

	@ExceptionHandler({Exception.class})
	ProblemDetail handleException(Exception ex) {
		log.warn(ex.getMessage(), ex);

		return ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
	}

}
