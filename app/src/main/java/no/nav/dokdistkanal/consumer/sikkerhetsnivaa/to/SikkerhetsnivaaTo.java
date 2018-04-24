package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor(access= AccessLevel.PUBLIC)
@NoArgsConstructor(access= AccessLevel.PUBLIC)
public class SikkerhetsnivaaTo {
	private boolean harLoggetPaaNivaa4;
	private String personIdent;
}
