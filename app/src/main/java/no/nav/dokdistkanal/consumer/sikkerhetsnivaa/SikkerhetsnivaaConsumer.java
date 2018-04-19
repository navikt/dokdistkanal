package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;

public interface SikkerhetsnivaaConsumer {
	SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) throws SikkerhetsnivaaFunctionalException;

	/**
	 * ping endpoint
	 */
	void ping();
}
