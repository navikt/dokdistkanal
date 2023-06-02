package no.nav.dokdistkanal.consumer.dokkat;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoToV4;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokkatTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.Boolean.FALSE;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.FunctionalUtils.createHeaders;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Slf4j
public class DokumentTypeInfoConsumer {
	public static final String HENT_DOKKAT_INFO = "hentDokumentTypeInfo";

	private final RestTemplate restTemplate;
	private final String dokumenttypeInfoUrl;
	private final String dokmetScope;
	private final TokenConsumer tokenConsumer;

	@Autowired
	public DokumentTypeInfoConsumer(@Value("${dokmet_scope}") String dokmetScope,
									@Value("${dokumenttypeInfo_url}") String dokumenttypeInfoUrl,
									RestTemplateBuilder restTemplateBuilder,
									TokenConsumer tokenConsumer) {
		this.dokmetScope = dokmetScope;
		this.dokumenttypeInfoUrl = dokumenttypeInfoUrl;
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20))
				.build();
	}

	public DokumentTypeInfoConsumer(RestTemplate restTemplate, TokenConsumer tokenConsumer) {
		this.dokmetScope = "";
		this.dokumenttypeInfoUrl = "";
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplate;
	}

	@Cacheable(value = HENT_DOKKAT_INFO, key = "#dokumenttypeId+'-dokkat'")
	@Retryable(retryFor = DokDistKanalTechnicalException.class, noRetryFor = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumentTypeInfoTo hentDokumenttypeInfo(final String dokumenttypeId) {
		TokenResponse clientCredentialToken = tokenConsumer.getClientCredentialToken(dokmetScope);
		HttpHeaders headers = createHeaders(clientCredentialToken.getAccess_token());

		try {
			HttpEntity<String> request = new HttpEntity(headers);
			DokumentTypeInfoToV4 response = restTemplate.exchange(this.dokumenttypeInfoUrl + "/" + dokumenttypeId, GET, request, DokumentTypeInfoToV4.class).getBody();
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (UNAUTHORIZED.equals(e.getStatusCode()) || FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException("DokumentTypeInfoConsumer feilet (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
			}
			throw new DokkatFunctionalException("DokumentTypeInfoConsumer feilet. (HttpStatus=" + e.getStatusCode() + ") for dokumenttypeId:" + dokumenttypeId, e);
		} catch (HttpServerErrorException e) {
			throw new DokkatTechnicalException("DokumentTypeInfoConsumer feilet med statusCode=" + e.getStatusCode(), e);
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
					.isVarslingSdp(FALSE).build();
		} else {
			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(
									distribusjonVarselTo -> SDP.toString()
											.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}
}