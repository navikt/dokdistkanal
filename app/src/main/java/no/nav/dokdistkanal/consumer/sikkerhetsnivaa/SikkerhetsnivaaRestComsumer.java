package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Slf4j
public class SikkerhetsnivaaRestComsumer implements SikkerhetsnivaaConsumer {

	private final RestTemplate restTemplate;
	public static final String HENT_PAALOGGINGSNIVAA = "hentPaaloggingsnivaa";

	public SikkerhetsnivaaRestComsumer(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Inject
	public SikkerhetsnivaaRestComsumer(RestTemplateBuilder restTemplateBuilder,
									   HttpComponentsClientHttpRequestFactory requestFactory,
									   SikkerhetsnivaaV1Alias sikkerhetsnivaaV1Alias,
									   ServiceuserAlias serviceuserAlias) {
		this.restTemplate = restTemplateBuilder
				.requestFactory(requestFactory)
				.rootUri(sikkerhetsnivaaV1Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(sikkerhetsnivaaV1Alias.getConnecttimeoutms())
				.setReadTimeout(sikkerhetsnivaaV1Alias.getReadtimeoutms())
				.build();
	}


	@Override
	@Cacheable(value=HENT_PAALOGGINGSNIVAA, key = "#fnr+'-sikkerhetsnivaa'")
	public SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) throws SikkerhetsnivaaFunctionalException {
		SikkerhetsnivaaRequest request = SikkerhetsnivaaRequest.builder().personidentifikator(fnr).build();
		try {
			SikkerhetsnivaaResponse response = restTemplate.postForObject("/", request, SikkerhetsnivaaResponse.class);
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
				throw new SikkerhetsnivaaTechnicalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=" + e.getStatusCode() + ")", e);
			} else {
				throw new SikkerhetsnivaaFunctionalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=" + e.getStatusCode() + ")", e);
			}
		} catch (Exception e) {
			throw new SikkerhetsnivaaTechnicalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet", e);
		}
	}

	@Override
	public void ping() {
		String ping = restTemplate.getForObject("isReady", String.class);
		Assert.isTrue(StringUtils.isNotBlank(ping), "Sikkerhetsnivaa ping failed " + ping);
	}

	private SikkerhetsnivaaTo mapTo(SikkerhetsnivaaResponse response) {
		return SikkerhetsnivaaTo.builder().personIdent(response.getPersonidentifikator()).harLoggetPaaNivaa4(response.isHarbruktnivaa4()).build();
	}
}
