package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SikkerhetsnivaaTo {
	private boolean harLoggetPaaNivaa4;
	private String personIdent;
}
