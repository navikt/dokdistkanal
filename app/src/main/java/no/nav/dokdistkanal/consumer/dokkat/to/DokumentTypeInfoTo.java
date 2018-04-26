package no.nav.dokdistkanal.consumer.dokkat.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DokumentTypeInfoTo {
	private String arkivsystem;
	private String predefinertDistKanal;
	private boolean isVarslingSdp;
}
