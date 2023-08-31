package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.springdoc.SwaggerBestemDistribusjonskanal;
import no.nav.dokdistkanal.service.BestemDistribusjonskanalService;
import no.nav.security.token.support.core.api.Protected;
import org.jboss.logging.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.dokdistkanal.common.MDCUtils.getOrCreateCallId;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;

@Slf4j
@Protected
@RestController
@RequestMapping("/rest")
@Tag(name = "bestemDistribusjonskanal", description = "Tjeneste for å bestemme distribusjonskanal")
public class BestemDistribusjonskanalController {

	private final BestemDistribusjonskanalService bestemDistribusjonskanalService;

	public BestemDistribusjonskanalController(BestemDistribusjonskanalService bestemDistribusjonskanalService) {
		this.bestemDistribusjonskanalService = bestemDistribusjonskanalService;
	}

	@SwaggerBestemDistribusjonskanal
	@PostMapping(value = "/bestemDistribusjonskanal")
	public ResponseEntity<BestemDistribusjonskanalResponse> bestemDistribusjonskanal(
			@Valid @RequestBody BestemDistribusjonskanalRequest request,
			@RequestHeader(value = NAV_CALLID, required = false) String navCallId) {

		MDC.put(CALL_ID, getOrCreateCallId(navCallId));

		return ResponseEntity.ok(bestemDistribusjonskanalService.bestemDistribusjonskanal(request));
	}
}
