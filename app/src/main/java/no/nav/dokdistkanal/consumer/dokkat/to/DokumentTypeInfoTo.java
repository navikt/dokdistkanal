package no.nav.dokdistkanal.consumer.dokkat.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DokumentTypeInfoTo implements Serializable {
	static final long serialVersionUID = 1L;
	private String arkivsystem;
	private String predefinertDistKanal;
	private boolean isVarslingSdp;
}
