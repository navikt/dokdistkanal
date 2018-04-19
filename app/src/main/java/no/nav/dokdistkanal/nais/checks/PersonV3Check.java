package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.config.fasit.PersonV3Alias;
import no.nav.dokdistkanal.nais.selftest.support.AbstractSelftest;
import no.nav.dokdistkanal.nais.selftest.support.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.support.Ping;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
public class PersonV3Check extends AbstractSelftest {
	public static final String PERSONV3_LABEL = "PersonV3";
	private final PersonV3 personV3;

	@Inject
	public PersonV3Check(PersonV3 personV3, PersonV3Alias personV3Alias) {
		super(Ping.Type.Soap,
				PERSONV3_LABEL,
				personV3Alias.getEndpointurl(),
				personV3Alias.getDescription() == null ? PERSONV3_LABEL : personV3Alias.getDescription());
		this.personV3 = personV3;
	}

	@Override
	protected void doCheck() {
		try {
			personV3.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Could not ping PersonV3", e);
		}
	}
}