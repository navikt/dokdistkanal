package no.nav.dokdistkanal.rest;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.PROCESSED_OK;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.RECEIVED;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.incrementFunctionalException;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.incrementSecurityException;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.incrementTechnicalException;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;
import static no.nav.dokdistkanal.rest.NavHeaders.CALL_ID;
import static no.nav.dokdistkanal.rest.NavHeaders.NAV_CALLID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.metrics.PrometheusLabels;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.UUID;

@Slf4j
@RestController
public class DokDistKanalRestController {
	public static final String BESTEM_DISTRIBUSJON_KANAL = "bestemDistribusjonKanal";
	private static final String REST = "/rest/";
	public static final String BESTEM_KANAL_URI_PATH = REST + "bestemKanal";
	private static final String MDC_CALL_ID = "callId";

	private final DokDistKanalService dokDistKanalService;

	@Inject
	public DokDistKanalRestController(DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@ResponseBody
	@PostMapping(value = BESTEM_KANAL_URI_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DokDistKanalResponse bestemKanal(@RequestBody DokDistKanalRequest request,
											@RequestHeader(value = NAV_CALLID, required = false) String navCallid,
											@RequestHeader(value = CALL_ID, required = false) String dokCallId) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		Histogram.Timer requestTimer = requestLatency.labels(BESTEM_DISTRIBUSJON_KANAL, "velgKanal", "velgKanal")
				.startTimer();
		try {
			MDC.put(MDC_CALL_ID, getOrCreateCallId(navCallid, dokCallId));
			requestCounter.labels(BESTEM_DISTRIBUSJON_KANAL, PrometheusLabels.REST, getConsumerId(), RECEIVED).inc();
			DokDistKanalResponse response = dokDistKanalService.velgKanal(request);
			requestCounter.labels(BESTEM_DISTRIBUSJON_KANAL, PrometheusLabels.REST, getConsumerId(), PROCESSED_OK).inc();
			return response;
		} catch (DokDistKanalFunctionalException e) {
			incrementFunctionalException(e);
			log.warn("Funksjonell feil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (DokDistKanalTechnicalException e) {
			incrementTechnicalException(e);
			log.error("Teknisk feil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (DokDistKanalSecurityException e) {
			incrementSecurityException(e);
			log.warn("Teknisk sikkerhetsfeil med melding: {}", e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			incrementTechnicalException(e);
			log.error("Ukjent teknisk feil.", e);
			throw e;
		} finally {
			requestTimer.observeDuration();
		}
	}

	private String getOrCreateCallId(final String navCallid, final String dokCallId) {
		if(isNotBlank(navCallid)) {
			return navCallid;
		}
		if(isNotBlank(dokCallId)) {
			return dokCallId;
		}
		return UUID.randomUUID().toString();
	}
}
