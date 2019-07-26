package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;

public interface SikkerhetsnivaaConsumer {
	SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr);

	/**
	 * ping endpoint
	 */
	void ping();
}
