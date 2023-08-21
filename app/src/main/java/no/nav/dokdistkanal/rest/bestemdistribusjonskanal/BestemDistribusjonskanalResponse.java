package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;

public record BestemDistribusjonskanalResponse(DistribusjonKanalCode distribusjonskanal,
											   String regel,
											   String regelBegrunnelse) {

	public BestemDistribusjonskanalResponse(BestemDistribusjonskanalRegel regel) {
		this(regel.distribusjonKanal, regel.name(), regel.begrunnelse);
	}
}
