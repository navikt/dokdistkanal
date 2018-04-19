package no.nav.dokdistkanal.consumer.dokkat.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokumentTypeInfoTo {
	private String arkivsystem;
	private String arkivbehandling;
}
