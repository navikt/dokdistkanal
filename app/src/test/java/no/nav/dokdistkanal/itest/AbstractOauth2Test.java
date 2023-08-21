package no.nav.dokdistkanal.itest;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@EnableMockOAuth2Server
public abstract class AbstractOauth2Test {

	private static final String AZUREV2_ISSUER = "azurev2";
	private static final String AZP_NAME = "dev-fss:teamdokumenthandtering:dokdistkanal";
	protected static final String OID = "4e5a62ad-a76d-4a67-8eac-8ff15b3b48fa";

	@Autowired
	public MockOAuth2Server mockOAuth2Server;

	public String jwt() {
		return jwt(Map.ofEntries(
				Map.entry("azp_name", AZP_NAME),
				Map.entry("oid", OID)));
	}

	private String jwt(Map<String, String> claims) {
		String audience = "dokdistkanal";
		return mockOAuth2Server.issueToken(
				AZUREV2_ISSUER,
				"gosys-clientid",
				new DefaultOAuth2TokenCallback(
						AZUREV2_ISSUER,
						"subject",
						"JWT",
						List.of(audience),
						claims,
						60
				)
		).serialize();
	}

}
