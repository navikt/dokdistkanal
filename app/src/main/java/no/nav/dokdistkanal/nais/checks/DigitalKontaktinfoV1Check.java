package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.config.fasit.DigitalKontaktinfoV1Alias;
import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DigitalKontaktinfoV1Check extends AbstractDependencyCheck {
	private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
	private static final String DIGITAL_KONTAKT_INFORMASJON_V1_NAME = "digitalKontaktInformasjonV1";

	@Inject
	public DigitalKontaktinfoV1Check(DigitalKontaktinformasjonV1 personV3,
									 DigitalKontaktinfoV1Alias digitalKontaktinfoV1Alias) {
		super(DependencyType.SOAP,
				DIGITAL_KONTAKT_INFORMASJON_V1_NAME,
				digitalKontaktinfoV1Alias.getEndpointurl(),
				Importance.WARNING);
		this.digitalKontaktinformasjonV1 = personV3;
	}

	@Override
	protected void doCheck() {
		try {
			digitalKontaktinformasjonV1.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", DIGITAL_KONTAKT_INFORMASJON_V1_NAME, getErrorMessage(e)), e);
		}
	}
}
