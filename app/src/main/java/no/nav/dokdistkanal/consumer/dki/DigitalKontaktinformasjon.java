package no.nav.dokdistkanal.consumer.dki;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;

public interface DigitalKontaktinformasjon {
	DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(String personidentifikator, boolean inkluderSikkerDigitalPost);
}
