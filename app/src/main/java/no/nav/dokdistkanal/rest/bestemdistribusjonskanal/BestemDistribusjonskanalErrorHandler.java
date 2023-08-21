package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.SikkerhetsnivaaFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.AltinnServiceOwnerTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.PdlTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.SikkerhetsnivaaTechnicalException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@Slf4j
@ControllerAdvice(basePackages = "no.nav.dokdistkanal.rest.bestemdistribusjonskanal")
public class BestemDistribusjonskanalErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({DokmetFunctionalException.class,
			AltinnServiceOwnerFunctionalException.class,
			DigitalKontaktinformasjonV2FunctionalException.class,
			PdlFunctionalException.class,
			SikkerhetsnivaaFunctionalException.class})
	ProblemDetail handleConsumerFunctionalException(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
		problem.setTitle("Funksjonell feil ved kall mot ekstern tjeneste");

		log.warn(problem.getDetail(), ex);

		return problem;
	}

	@ExceptionHandler({DokmetTechnicalException.class,
			AltinnServiceOwnerTechnicalException.class,
			DigitalKontaktinformasjonV2TechnicalException.class,
			PdlTechnicalException.class,
			SikkerhetsnivaaTechnicalException.class})
	ProblemDetail handleConsumerTechnicalException(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
		problem.setTitle("Teknisk feil ved kall mot ekstern tjeneste");

		log.warn(problem.getDetail(), ex);

		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String feilmelding = ex.getBindingResult().getFieldErrors().stream()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.collect(Collectors.joining(", "));

		log.warn(feilmelding, ex);

		return ResponseEntity
				.status(BAD_REQUEST)
				.contentType(APPLICATION_PROBLEM_JSON)
				.body(ProblemDetail.forStatusAndDetail(BAD_REQUEST, feilmelding));
	}


	@ExceptionHandler({Exception.class})
	ProblemDetail handleException(Exception ex) {
		log.warn(ex.getMessage(), ex);

		return ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, ex.getMessage());
	}

}
