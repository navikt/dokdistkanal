package no.nav.dokdistkanal.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.rest.NavHeaders.NAV_CALLID;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/rest")
public class DokDistKanalRestController {
	public static final String BESTEM_DISTRIBUSJON_KANAL = "bestemDistribusjonKanal";
	public static final String BESTEM_KANAL_URI_PATH = "/bestemKanal";

	private final DokDistKanalService dokDistKanalService;

	public DokDistKanalRestController(DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@PostMapping(value = "/bestemKanal", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public DokDistKanalResponse bestemKanal(@RequestBody DokDistKanalRequest request,
											@RequestHeader(value = NAV_CALLID, required = false) String navCallid,
											@RequestHeader(value = CALL_ID, required = false) String dokCallId) {
		try {
			MDC.put(CALL_ID, getOrCreateCallId(navCallid, dokCallId));
			return dokDistKanalService.velgKanal(request);
		} catch (DokDistKanalFunctionalException e) {
			// ingen stacktrace på funksjonelle feil
			log.warn("Funksjonell feil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (DokDistKanalTechnicalException e) {
			log.error("Teknisk feil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (DokDistKanalSecurityException e) {
			log.warn("Teknisk sikkerhetsfeil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			log.error("Ukjent teknisk feil.", e);
			throw e;
		}
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
