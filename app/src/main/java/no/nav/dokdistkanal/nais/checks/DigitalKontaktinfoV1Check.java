package no.nav.dokdistkanal.nais.checks;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;

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

	@Inject
	public DigitalKontaktinfoV1Check(DigitalKontaktinformasjonV1 personV3,
									 DigitalKontaktinfoV1Alias digitalKontaktinfoV1Alias) {
		super(DependencyType.SOAP,
				DIGITALKONTAKTINFORMASJONV1,
				digitalKontaktinfoV1Alias.getEndpointurl(),
				Importance.WARNING);
		this.digitalKontaktinformasjonV1 = personV3;
	}

	@Override
	protected void doCheck() {
		try {
			digitalKontaktinformasjonV1.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", DIGITALKONTAKTINFORMASJONV1, getErrorMessage(e)), e);
		}
	}
}
