package no.nav.dokdistkanal.consumer.dokkat;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.SERVICE_CODE_DOKDIST;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ketill Fenne, Visma Consulting AS
 */
@Service
@Slf4j
public class DokumentTypeInfoConsumer {
	private final RestTemplate restTemplate;
	public static final String HENT_DOKKAT_INFO = "hentDokumentTypeInfo";
	public static final String DOKKAT = "DOKKAT";
	private Histogram.Timer requestTimer;

	@Inject
	public DokumentTypeInfoConsumer(RestTemplateBuilder restTemplateBuilder,
									HttpComponentsClientHttpRequestFactory requestFactory,
									DokumenttypeInfoV4Alias dokumenttypeInfoV4Alias,
									ServiceuserAlias serviceuserAlias) {
		this.restTemplate = restTemplateBuilder
				.requestFactory(requestFactory)
				.rootUri(dokumenttypeInfoV4Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(dokumenttypeInfoV4Alias.getConnecttimeoutms())
				.setReadTimeout(dokumenttypeInfoV4Alias.getReadtimeoutms())
				.build();
	}

	public DokumentTypeInfoConsumer(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Cacheable(value = HENT_DOKKAT_INFO, key = "#dokumenttypeId+'-dokkat'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {

		requestCounter.labels(HENT_DOKKAT_INFO, CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();
		try {
			Map<String, Object> uriVariables = new HashMap<>();
			uriVariables.put("dokumenttypeId", dokumenttypeId);
			requestTimer = requestLatency.labels(SERVICE_CODE_DOKDIST, DOKKAT, HENT_DOKKAT_INFO).startTimer();
			DokumentTypeInfoToV4 dokumentTypeInfoToV4 = restTemplate.getForObject("/{dokumenttypeId}", DokumentTypeInfoToV4.class, uriVariables);
			return mapTo(dokumentTypeInfoToV4);
		} catch (HttpClientErrorException e) {
			if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) || HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException("DokumentTypeInfoConsumer feilet (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
			}
			throw new DokDistKanalFunctionalException("DokumentTypeInfoConsumer feilet. (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
		} catch (HttpServerErrorException e) {
			throw new DokDistKanalTechnicalException("DokumentTypeInfoConsumer feilet med statusCode=" + e.getRawStatusCode(), e);
		} catch (Exception e) {
			throw new DokDistKanalTechnicalException("DokumentTypeInfoConsumer feilet med message=" + e.getMessage(), e);
		} finally {
			requestTimer.observeDuration();
		}
	}

	private DokumentTypeInfoTo mapTo(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		String predefinertDistribusjonKanal = null;
		if (!(dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() == null)) {
			predefinertDistribusjonKanal = dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getPredefinertDistKanal();
		}

		if (dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() == null
				|| dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null) {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(Boolean.FALSE).build();
		} else {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels().stream()
							.anyMatch(
									distribusjonVarselTo -> DistribusjonKanalCode.SDP.toString()
											.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}
}