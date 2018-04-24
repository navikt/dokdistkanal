package no.nav.dokdistkanal.consumer.dokkat.to;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;

@Data
@Builder
@AllArgsConstructor(access= AccessLevel.PUBLIC)
@NoArgsConstructor(access= AccessLevel.PUBLIC)
public class DokumentTypeInfoTo {
	private String arkivbehandling;
	private boolean isVarslingSdp;
}
