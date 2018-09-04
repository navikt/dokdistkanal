package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.config.fasit.PersonV3Alias;
import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
public class PersonV3Check extends AbstractDependencyCheck {
	private static final String PERSONV3_LABEL = "PersonV3";
	private final PersonV3 personV3;

	@Inject
	public PersonV3Check(PersonV3 personV3, PersonV3Alias personV3Alias) {
		super(DependencyType.SOAP,
				PERSONV3_LABEL,
				personV3Alias.getEndpointurl(),
				Importance.WARNING);
		this.personV3 = personV3;
	}

	@Override
	protected void doCheck() {
		try {
			personV3.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", PERSONV3_LABEL, getErrorMessage(e)), e);
		}
	}
}