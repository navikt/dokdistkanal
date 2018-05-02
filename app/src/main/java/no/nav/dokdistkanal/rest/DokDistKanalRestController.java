package no.nav.dokdistkanal.rest;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_DOKDIST;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_SECURITY_EXCEPTION;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_TECHNICAL_EXCEPTION;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.PROCESSED_OK;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.RECEIVED;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestExceptionCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
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

@RestController
public class DokDistKanalRestController {

	private static final String REST = "/rest/";
	public static final String BESTEM_KANAL_URI_PATH = REST + "bestemKanal";
	private static final String CALL_ID = "callId";
	private Histogram.Timer requestTimer;

	private final DokDistKanalService dokDistKanalService;

	@Inject
	public DokDistKanalRestController(DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@ResponseBody
	@PostMapping(value = BESTEM_KANAL_URI_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DokDistKanalResponse bestemKanal(@RequestBody DokDistKanalRequest request,
											@RequestHeader(value = CALL_ID, required = false) String callId) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		requestTimer = requestLatency.labels(LABEL_DOKDIST, "velgKanal", "velgKanal")
				.startTimer();
		try {
			MDC.put(CALL_ID, callId);
			requestCounter.labels(LABEL_DOKDIST, PrometheusLabels.REST, getConsumerId(), RECEIVED)
					.inc();
			DokDistKanalResponse response = dokDistKanalService.velgKanal(request.getDokumentTypeId(), request.getMottakerId());
			requestCounter.labels(LABEL_DOKDIST, PrometheusLabels.REST, getConsumerId(), PROCESSED_OK).inc();
			return response;
		} catch (Exception e) {
			incrementExceptionMetrics(e);
			throw e;
		} finally {
			requestTimer.observeDuration();
		}
	}

	private void incrementExceptionMetrics(Exception e) {
		if (e instanceof DokDistKanalFunctionalException) {
			requestExceptionCounter.labels(LABEL_FUNCTIONAL_EXCEPTION, e.getClass()
					.getSimpleName(), ((DokDistKanalFunctionalException) e).getShortDescription()).inc();
		} else if (e instanceof DokDistKanalSecurityException) {
			requestExceptionCounter.labels(LABEL_SECURITY_EXCEPTION, e.getClass()
					.getSimpleName(), ((DokDistKanalSecurityException) e).getShortDescription()).inc();
		} else if (e instanceof DokDistKanalTechnicalException) {
			requestExceptionCounter.labels(LABEL_TECHNICAL_EXCEPTION, e.getClass()
					.getSimpleName(), ((DokDistKanalTechnicalException) e).getShortDescription()).inc();
		} else {
			requestExceptionCounter.labels(LABEL_TECHNICAL_EXCEPTION, e.getClass().getSimpleName(), e.getClass()
					.getSimpleName()).inc();
		}
	}


}
