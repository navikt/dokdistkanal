package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

@Component
public class DokumenttypeInfoV4Check extends AbstractDependencyCheck {

	private static final String DOKUMENTTYPEINFO_V4 = "DokumenttypeInfo_v3";
	private final RestTemplate restTemplate;

	@Inject
	public DokumenttypeInfoV4Check(RestTemplateBuilder restTemplateBuilder,
//								   HttpComponentsClientHttpRequestFactory requestFactory,
								   DokumenttypeInfoV4Alias dokumenttypeInfoV4Alias,
								   ServiceuserAlias serviceuserAlias) {
		super(DependencyType.REST,
				DOKUMENTTYPEINFO_V4,
				dokumenttypeInfoV4Alias.getUrl(),
				Importance.CRITICAL);
		this.restTemplate = restTemplateBuilder
//				.requestFactory(() -> requestFactory)
				.rootUri(dokumenttypeInfoV4Alias.getUrl())
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(Duration.ofMillis(dokumenttypeInfoV4Alias.getConnecttimeoutms()))
				.setReadTimeout(Duration.ofMillis(dokumenttypeInfoV4Alias.getReadtimeoutms()))
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/ping", String.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", DOKUMENTTYPEINFO_V4, getErrorMessage(e)), e);
		}
	}

}
