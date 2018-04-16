package no.nav.dokkanalvalg.consumer.sikkerhetsnivaa;

import no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

public class SikkerhetsnivaaRestComsumer implements SikkerhetsnivaaConsumer{
	@Inject
	RestTemplate restTemplate;

	private String sikkerhetsnivaaaUrl;

	@Override
	public SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) throws SikkerhetsnivaaFunctionalException {
		SikkerhetsnivaaRequest request = SikkerhetsnivaaRequest.builder().personidentifikator(fnr).build();

		SikkerhetsnivaaResponse response = restTemplate.postForObject(sikkerhetsnivaaaUrl, request, SikkerhetsnivaaResponse.class);

		return mapTo(response);
	}

	@Override
	public void ping() {
		String ping = restTemplate.getForObject(sikkerhetsnivaaaUrl + "isReady", String.class);
		Assert.isTrue(StringUtils.isNotBlank(ping), "Sikkerhetsnivaa ping failed " + ping);
	}

	@Inject
	public void setSikkerhetsnivaaUrl(@Value("${sikkerhetsnivaaa_v1_url}") String url) {
		this.sikkerhetsnivaaaUrl = url;
		if (!this.sikkerhetsnivaaaUrl.endsWith("/")) {
			this.sikkerhetsnivaaaUrl += "/";
		}
	}

	private SikkerhetsnivaaTo mapTo(SikkerhetsnivaaResponse response) {
		return SikkerhetsnivaaTo.builder().personIdent(response.getPersonidentifikator()).harLoggetPaaNivaa4(response.isHarbruktnivaa4()).build();
	}
}
