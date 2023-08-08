package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.service.DokDistKanalService;
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
public class BestemDistribusjonskanalController {

	private final DokDistKanalService dokDistKanalService;

	public BestemDistribusjonskanalController(DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@PostMapping(value = "/bestemDistribusjonskanal")
	public ResponseEntity<DokDistKanalResponse> bestemDistribusjonskanal(
			@RequestBody DokDistKanalRequest request,
			@RequestHeader(value = NAV_CALLID, required = false) String navCallId) {

		MDC.put(CALL_ID, getOrCreateCallId(navCallId));

		return ResponseEntity.ok(dokDistKanalService.velgKanal(request));
	}
}
