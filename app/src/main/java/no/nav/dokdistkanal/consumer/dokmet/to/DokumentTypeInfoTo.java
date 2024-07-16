package no.nav.dokdistkanal.consumer.dokmet.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DokumentTypeInfoTo {
	private String dokumentType;
	private String arkivSystem;
	private DokumentProduksjonsInfoTo dokumentProduksjonsInfo;
}
