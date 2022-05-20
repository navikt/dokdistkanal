package no.nav.dokdistkanal.consumer.dki;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;

public interface DigitalKontaktinformasjon {

	void pingDkif();
	DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(String personidentifikator, boolean inkluderSikkerDigitalPost);
}
