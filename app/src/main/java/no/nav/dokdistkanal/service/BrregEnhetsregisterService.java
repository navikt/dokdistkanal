package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.HentEnhetResponse;
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

	public boolean erEnhetenKonkurs(String orgNummer) {
		HentEnhetResponse hentEnhetResponse = brregEnhetsregisterConsumer.hentEnhet(orgNummer);
		HentEnhetResponse hentEnhet = hentEnhetResponse != null ? hentEnhetResponse : brregEnhetsregisterConsumer.hentHovedenhetFraUnderenhet(orgNummer);

		return hentEnhet == null || hentEnhet.konkurs();
	}

	public boolean harEnhetenGyldigRolletypeForDpvt(String orgNummer) {

		EnhetsRolleResponse response = brregEnhetsregisterConsumer.hentEnhetsRollegrupper(orgNummer);

		if (response == null || isEmpty(response.rollegrupper())) {
			return false;
		}

		return response.rollegrupper().stream()
				.flatMap(roller -> roller.roller().stream())
				.filter(Objects::nonNull)
				.filter(rolle -> !erPersonDoedEllerManglerFodselsdato(rolle.person()))
				.anyMatch(r -> GYLDIG_ROLLETYPE_FOR_DPVT.contains(r.type().kode()));
	}

	private boolean erPersonDoedEllerManglerFodselsdato(EnhetsRolleResponse.Person person) {
		if (person == null) {
			return false;
		}
		return person.erDoed() || person.fodselsdato() == null;
	}
}
