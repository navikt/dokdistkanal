package no.nav.dokkanalvalg.nais.checks;

import static no.nav.dokkanalvalg.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;

import no.nav.dokkanalvalg.config.fasit.DigitalKontaktinfoV1Alias;
import no.nav.dokkanalvalg.nais.selftest.support.AbstractSelftest;
import no.nav.dokkanalvalg.nais.selftest.support.ApplicationNotReadyException;
import no.nav.dokkanalvalg.nais.selftest.support.Ping;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DigitalKontaktinfoV1Check  extends AbstractSelftest {
	private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

	@Inject
	public DigitalKontaktinfoV1Check(DigitalKontaktinformasjonV1 personV3, DigitalKontaktinfoV1Alias digitalKontaktinfoV1Alias) {
		super(Ping.Type.Soap,
				DIGITALKONTAKTINFORMASJONV1,
				digitalKontaktinfoV1Alias.getEndpointurl(),
				digitalKontaktinfoV1Alias.getDescription() == null ? DIGITALKONTAKTINFORMASJONV1 : digitalKontaktinfoV1Alias.getDescription());
		this.digitalKontaktinformasjonV1 = personV3;
	}

	@Override
	protected void doCheck() {
		try {
			digitalKontaktinformasjonV1.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Could not ping "+ DIGITALKONTAKTINFORMASJONV1, e);
		}
	}
}
