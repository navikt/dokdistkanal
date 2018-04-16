package no.nav.dokkanalvalg.consumer.sikkerhetsnivaa;

import no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;

public interface SikkerhetsnivaaConsumer {
	SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) throws SikkerhetsnivaaFunctionalException;

	/**
	 * ping endpoint
	 */
	void ping();
}
