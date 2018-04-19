package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SikkerhetsnivaaTo {
	private boolean harLoggetPaaNivaa4;
	private String personIdent;
}
