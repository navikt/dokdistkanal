package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjon;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DkifConsumerCheck extends AbstractDependencyCheck {

	private static final String DKIF_CONSUMER = "DkifConsumer";
	private final DigitalKontaktinformasjon digitalKontaktinformasjon;

	@Autowired
	public DkifConsumerCheck(DigitalKontaktinformasjon digitalKontaktinformasjon, @Value("${digdir_krr_proxy_url}") String dkiUrl) {
		super(DependencyType.REST,
				DKIF_CONSUMER,
				dkiUrl,
				Importance.CRITICAL);
		this.digitalKontaktinformasjon = digitalKontaktinformasjon;
	}

	@Override
	protected void doCheck() {
		try {
			digitalKontaktinformasjon.pingDkif();
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", DKIF_CONSUMER, getErrorMessage(e)), e);
		}
	}
}
