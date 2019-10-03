package no.nav.dokdistkanal.consumer.tps;

import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting.
 */
public interface Tps {
	TpsHentPersoninfoForIdentTo tpsHentPersoninfoForIdent(String personidentifikator);
}
