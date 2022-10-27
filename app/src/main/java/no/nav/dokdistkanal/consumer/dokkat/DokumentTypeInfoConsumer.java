package no.nav.dokdistkanal.consumer.dokkat;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoToV4;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokkatTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static no.nav.dokdistkanal.constants.DomainConstants.APP_NAME;
import static no.nav.dokdistkanal.constants.MDCConstants.*;
import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;
import static org.springframework.http.HttpMethod.GET;

@Service
@Slf4j
public class DokumentTypeInfoConsumer {
	private final RestTemplate restTemplate;
	public static final String HENT_DOKKAT_INFO = "hentDokumentTypeInfo";
	private final String dokumenttypeInfoUrl;
	private final String dokmetScope;
	private final TokenConsumer tokenConsumer;
	private final MicrometerMetrics metrics;

	@Autowired
	public DokumentTypeInfoConsumer(@Value("${dokmet_scope}") String dokmetScope,
									@Value("${dokumenttypeInfo_url}") String dokumenttypeInfoUrl,
									RestTemplateBuilder restTemplateBuilder,
									MicrometerMetrics metrics,
									TokenConsumer tokenConsumer) {
		this.dokmetScope = dokmetScope;
		this.dokumenttypeInfoUrl = dokumenttypeInfoUrl;
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20))
				.build();
		this.metrics = metrics;
	}

	public DokumentTypeInfoConsumer(RestTemplate restTemplate, MicrometerMetrics metrics, TokenConsumer tokenConsumer) {
		this.dokmetScope = "";
		this.dokumenttypeInfoUrl = "";
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplate;
		this.metrics = metrics;
	}

	@Cacheable(value = HENT_DOKKAT_INFO, key = "#dokumenttypeId+'-dokkat'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_DOKKAT_INFO}, percentiles = {0.5, 0.95}, histogram = true)
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) {
		HttpHeaders headers = createHeaders();

		metrics.cacheMiss(HENT_DOKKAT_INFO);
		try {
			HttpEntity<String> request = new HttpEntity(headers);
			DokumentTypeInfoToV4 response = restTemplate.exchange(this.dokumenttypeInfoUrl + "/" + dokumenttypeId, GET, request, DokumentTypeInfoToV4.class).getBody();
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) || HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException("DokumentTypeInfoConsumer feilet (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
			}
			throw new DokkatFunctionalException("DokumentTypeInfoConsumer feilet. (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
		} catch (HttpServerErrorException e) {
			throw new DokkatTechnicalException("DokumentTypeInfoConsumer feilet med statusCode=" + e.getRawStatusCode(), e);
		} catch (Exception e) {
			throw new DokkatTechnicalException("DokumentTypeInfoConsumer feilet med message=" + e.getMessage(), e);
		}
	}

	private DokumentTypeInfoTo mapTo(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		String predefinertDistribusjonKanal = null;
		if (!(dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
				.getDistribusjonInfo() == null)) {
			predefinertDistribusjonKanal = dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
					.getDistribusjonInfo()
					.getPredefinertDistKanal();
		}

		if (dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null || dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
				.getDistribusjonInfo() == null
				|| dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null) {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(Boolean.FALSE).build();
		} else {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(
									distribusjonVarselTo -> DistribusjonKanalCode.SDP.toString()
											.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}

	private HttpHeaders createHeaders() {
		TokenResponse clientCredentialToken = tokenConsumer.getClientCredentialToken(dokmetScope);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(clientCredentialToken.getAccess_token());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}