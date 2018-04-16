package no.nav.dokkanalvalg.consumer.sikkerhetsnivaa.schema;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SikkerhetsnivaaRequest {
	private String personidentifikator;
}
