package no.nav.dokdistkanal.consumer.dokkat;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.SERVICE_CODE_TREG001;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV3Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.DokKanalvalgFunctionalException;
import no.nav.dokdistkanal.exceptions.DokKanalvalgTechnicalException;
import no.nav.dokkat.api.tkat020.v3.DokumentTypeInfoToV3;
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
									DokumenttypeInfoV3Alias dokumenttypeInfoV3Alias,
									ServiceuserAlias serviceuserAlias) {
		this.restTemplate = restTemplateBuilder
				.requestFactory(requestFactory)
				.rootUri(dokumenttypeInfoV3Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(dokumenttypeInfoV3Alias.getConnecttimeoutms())
				.setReadTimeout(dokumenttypeInfoV3Alias.getReadtimeoutms())
				.build();
	}

	public DokumentTypeInfoConsumer(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Cacheable(HENT_DOKKAT_INFO)
	@Retryable(include = DokKanalvalgTechnicalException.class, exclude = {DokKanalvalgFunctionalException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) throws DokKanalvalgFunctionalException,DokKanalvalgTechnicalException{

		requestCounter.labels(HENT_DOKKAT_INFO, LABEL_CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();
		
		try {
			Map<String, Object> uriVariables = new HashMap<>();
			uriVariables.put("dokumenttypeId", dokumenttypeId);
			requestTimer = requestLatency.labels(SERVICE_CODE_TREG001, DOKKAT, HENT_DOKKAT_INFO).startTimer();
			DokumentTypeInfoToV3 dokumentTypeInfoToV3 =  restTemplate.getForObject("/{dokumenttypeId}", DokumentTypeInfoToV3.class, uriVariables);
			if (dokumentTypeInfoToV3.getDokumentMottakInfo() == null) {
				return null;
			} else {
				return mapTo(dokumentTypeInfoToV3);
			}
		} catch (HttpClientErrorException e) {
			if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
				throw new DokKanalvalgFunctionalException("Dokkat.TKAT020 failed with bad request for dokumenttypeId:" + dokumenttypeId, e);
			} else {
				throw new DokKanalvalgTechnicalException("Dokkat.TKAT020 failed. (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
			}
		} catch (HttpServerErrorException e) {
			throw new DokKanalvalgTechnicalException("Dokkat.TKAT020 failed with statusCode=" + e.getRawStatusCode(), e);
		} catch (Exception e) {
			throw new DokKanalvalgTechnicalException("Dokkat.TKAT020 failed with message=" + e.getMessage(), e);
		} finally {
			requestTimer.observeDuration();
		}
	}

	private DokumentTypeInfoTo mapTo(DokumentTypeInfoToV3 dokumentTypeInfoToV3) {
		return DokumentTypeInfoTo.builder()
				.arkivbehandling(dokumentTypeInfoToV3.getDokumentMottakInfo().getArkivBehandling())
				.arkivsystem(dokumentTypeInfoToV3.getDokumentMottakInfo().getArkivSystem()).build();
	}
}