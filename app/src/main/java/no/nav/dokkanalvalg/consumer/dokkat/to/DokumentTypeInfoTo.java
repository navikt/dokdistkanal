package no.nav.dokkanalvalg.consumer.dokkat.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokumentTypeInfoTo {
	private String arkivsystem;
	private String arkivbehandling;
}
