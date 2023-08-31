package no.nav.dokdistkanal.rest.bestemkanal;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.service.DokDistKanalService;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Unprotected
@RestController
@RequestMapping("/rest")
public class DokDistKanalRestController {
	public static final String BESTEM_DISTRIBUSJON_KANAL = "bestemDistribusjonKanal";
	public static final String BESTEM_KANAL_URI_PATH = "/bestemKanal";

	private final DokDistKanalService dokDistKanalService;

	public DokDistKanalRestController(DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@PostMapping(value = BESTEM_KANAL_URI_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public DokDistKanalResponse bestemKanal(@RequestBody DokDistKanalRequest request,
											@RequestHeader(value = NAV_CALLID, required = false) String navCallid,
											@RequestHeader(value = CALL_ID, required = false) String dokCallId) {

		MDC.put(CALL_ID, getOrCreateCallId(navCallid, dokCallId));
		return dokDistKanalService.velgKanal(request);
	}

	private String getOrCreateCallId(final String navCallid, final String dokCallId) {
		if (isNotNullOrEmpty(navCallid)) {
			return navCallid;
		}
		if (isNotNullOrEmpty(dokCallId)) {
			return dokCallId;
		}
		return UUID.randomUUID().toString();
	}

	private boolean isNotNullOrEmpty(String callId) {
		return (callId != null && !callId.isEmpty());
	}
}
