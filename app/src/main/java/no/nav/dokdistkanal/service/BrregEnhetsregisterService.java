package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.HentUnderenhetResponse;
import no.nav.dokdistkanal.consumer.brreg.HovedenhetResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class BrregEnhetsregisterService {

	private static final Set<String> GYLDIG_ROLLETYPE_FOR_DPVT = Set.of("DAGL", "INNH", "LEDE", "BEST", "DTPR", "DTSO");


	private final BrregEnhetsregisterConsumer brregEnhetsregisterConsumer;

	public BrregEnhetsregisterService(BrregEnhetsregisterConsumer brregEnhetsregisterConsumer) {
		this.brregEnhetsregisterConsumer = brregEnhetsregisterConsumer;
	}

	public HovedenhetResponse hentHovedenhet(String organisasjonsnummer) {
		HentUnderenhetResponse hentHovedenhetFraUnderenhet = brregEnhetsregisterConsumer.hentHovedenhetFraUnderenhet(organisasjonsnummer);

		if (hentHovedenhetFraUnderenhet != null && hentHovedenhetFraUnderenhet.slettedato() != null) {
			return new HovedenhetResponse(hentHovedenhetFraUnderenhet.organisasjonsnummer(), false, hentHovedenhetFraUnderenhet.slettedato());
		}

		final String orgNr = erOverordnetEnhetNotNull(hentHovedenhetFraUnderenhet) ? hentHovedenhetFraUnderenhet.overordnetEnhet() : organisasjonsnummer;
		return brregEnhetsregisterConsumer.hentHovedenhet(orgNr);
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

	private boolean erOverordnetEnhetNotNull(HentUnderenhetResponse hentUnderenhetResponse) {
		return hentUnderenhetResponse != null && isNotBlank(hentUnderenhetResponse.overordnetEnhet());
	}
}
