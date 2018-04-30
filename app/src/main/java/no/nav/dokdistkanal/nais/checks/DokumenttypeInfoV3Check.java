package no.nav.dokdistkanal.nais.checks;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;

import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
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

	private final RestTemplate restTemplate;

	@Inject
	public DokumenttypeInfoV3Check(RestTemplateBuilder restTemplateBuilder,
								   HttpComponentsClientHttpRequestFactory requestFactory,
								   DokumenttypeInfoV4Alias dokumenttypeInfoV4Alias,
								   ServiceuserAlias serviceuserAlias) {
		super(Ping.Type.Rest,
				DIGITALKONTAKTINFORMASJONV1,
				dokumenttypeInfoV4Alias.getUrl(),
				dokumenttypeInfoV4Alias.getDescription() == null ? DIGITALKONTAKTINFORMASJONV1 : dokumenttypeInfoV4Alias.getDescription());
		this.restTemplate = restTemplateBuilder.requestFactory(requestFactory)
				.rootUri(dokumenttypeInfoV4Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(dokumenttypeInfoV4Alias.getConnecttimeoutms())
				.setReadTimeout(dokumenttypeInfoV4Alias.getReadtimeoutms())
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
