package no.nav.dokdistkanal.nais.checks;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;

import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV3Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.nais.selftest.support.AbstractSelftest;
import no.nav.dokdistkanal.nais.selftest.support.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.support.Ping;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Component
public class DokumenttypeInfoV3Check extends AbstractSelftest {

	private final String url;
	private final RestTemplate restTemplate;

	@Inject
	public DokumenttypeInfoV3Check(RestTemplateBuilder restTemplateBuilder,
								   HttpComponentsClientHttpRequestFactory requestFactory,
								   DokumenttypeInfoV3Alias dokumenttypeInfoV3Alias,
								   ServiceuserAlias serviceuserAlias) {
		super(Ping.Type.Rest,
				DIGITALKONTAKTINFORMASJONV1,
				dokumenttypeInfoV3Alias.getUrl(),
				dokumenttypeInfoV3Alias.getDescription() == null ? DIGITALKONTAKTINFORMASJONV1 : dokumenttypeInfoV3Alias.getDescription());
		this.url = dokumenttypeInfoV3Alias.getUrl();
		this.restTemplate = restTemplateBuilder.requestFactory(requestFactory)
				.rootUri(dokumenttypeInfoV3Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(dokumenttypeInfoV3Alias.getConnecttimeoutms())
				.setReadTimeout(dokumenttypeInfoV3Alias.getReadtimeoutms())
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/ping", String.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Could not ping "+ DIGITALKONTAKTINFORMASJONV1, e);
		}
	}

}
