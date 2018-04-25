package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class SikkerhetsnivaaTo {
	private boolean harLoggetPaaNivaa4;
	private String personIdent;
}
