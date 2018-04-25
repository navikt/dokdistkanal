package no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SikkerhetsnivaaRequest {
	private String personidentifikator;
}
