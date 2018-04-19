package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema;

import lombok.Data;

@Data
public class SikkerhetsnivaaResponse {
	private boolean harbruktnivaa4;
	private String personidentifikator;
}
