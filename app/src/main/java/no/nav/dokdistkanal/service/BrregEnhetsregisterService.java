package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.HentUnderenhetResponse;
import no.nav.dokdistkanal.consumer.brreg.HovedenhetResponse;
import no.nav.dokdistkanal.domain.Enhetsopplysninger;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Hovedenhet;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Underenhet;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class BrregEnhetsregisterService {

	private static final Set<String> GYLDIG_ROLLETYPE_FOR_DPVT = Set.of("DAGL", "INNH", "LEDE", "BEST", "DTPR", "DTSO");


	private final BrregEnhetsregisterConsumer brregEnhetsregisterConsumer;

	public BrregEnhetsregisterService(BrregEnhetsregisterConsumer brregEnhetsregisterConsumer) {
		this.brregEnhetsregisterConsumer = brregEnhetsregisterConsumer;
	}

	public Enhetsopplysninger hentEnhetsopplysninger(String organisasjonsnummer) {
		Underenhet underenhet = hentUnderenhet(organisasjonsnummer);

		if (underenhet == null) {
			return Enhetsopplysninger.fraHovedenhet(hentHovedenhet(organisasjonsnummer));
		}

		if (underenhet.erSlettet()) {
			return Enhetsopplysninger.fraUnderenhet(underenhet);
		}

		Hovedenhet hovedenhet = hentHovedenhet(underenhet.hovedenhetOrganisasjonsnummer());

		return new Enhetsopplysninger(underenhet, hovedenhet);
	}

	public boolean harEnhetenGyldigRolletypeForDpvt(String organisasjonsnummer) {

		EnhetsRolleResponse response = brregEnhetsregisterConsumer.hentEnhetsRollegrupper(organisasjonsnummer);

		if (response == null || isEmpty(response.rollegrupper())) {
			return false;
		}

		return response.rollegrupper().stream()
				.flatMap(roller -> roller.roller().stream())
				.filter(Objects::nonNull)
				.filter(rolle -> !rolle.fratraadt())
				.filter(rolle -> !erPersonDoedEllerManglerFodselsdato(rolle.person()))
				.anyMatch(r -> GYLDIG_ROLLETYPE_FOR_DPVT.contains(r.type().kode()));
	}

	private boolean erPersonDoedEllerManglerFodselsdato(EnhetsRolleResponse.Person person) {
		if (person == null) {
			return false;
		}
		return person.erDoed() || person.fodselsdato() == null;
	}

	private Underenhet hentUnderenhet(String organisasjonsnummer) {
		HentUnderenhetResponse underenhetResponse = brregEnhetsregisterConsumer.hentUnderenhet(organisasjonsnummer);
		return mapUnderenhet(underenhetResponse);
	}

	private Hovedenhet hentHovedenhet(String organisasjonsnummer) {
		HovedenhetResponse hovedenhetResponse = brregEnhetsregisterConsumer.hentHovedenhet(organisasjonsnummer);
		return mapHovedenhet(hovedenhetResponse);
	}

	private static Underenhet mapUnderenhet(HentUnderenhetResponse underenhet) {
		if (underenhet == null) {
			return null;
		}
		return new Underenhet(underenhet.organisasjonsnummer(), underenhet.overordnetEnhet(), underenhet.slettedato());
	}

	private static Hovedenhet mapHovedenhet(HovedenhetResponse hovedenhet) {
		if (hovedenhet == null) {
			return null;
		}
		return new Hovedenhet(hovedenhet.organisasjonsnummer(), hovedenhet.konkurs(), hovedenhet.slettedato());
	}
}
