package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SikkerhetsnivaaTo implements Serializable {
	static final long serialVersionUID = 1L;
	private boolean harLoggetPaaNivaa4;
	private String personIdent;
}
