package no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.schema;

import lombok.Builder;
import lombok.Data;

@Data
public class SikkerhetsnivaaResponse {
	private boolean harbruktnivaa4;
	private String personidentifikator;
}
