package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Autowired;

public class DkifConsumerCheck extends AbstractDependencyCheck {

	private static final String DKIF_CONSUMER = "DkifConsumer";
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;

	@Autowired
	public DkifConsumerCheck(DokumenttypeInfoV4Alias dokumenttypeInfoV4Alias,
							 DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer) {
		super(DependencyType.REST,
				DKIF_CONSUMER,
				dokumenttypeInfoV4Alias.getUrl(),
				Importance.CRITICAL);
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
	}

	@Override
	protected void doCheck() {
		try {
			digitalKontaktinformasjonConsumer.pingDkif();
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", DKIF_CONSUMER, getErrorMessage(e)), e);
		}
	}
}
