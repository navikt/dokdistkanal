package no.nav.dokdistkanal.consumer.dokmet.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DokumentTypeInfoToV4 {
	private String dokumentType;
	private String arkivSystem;
	private DokumentProduksjonsInfoToV4 dokumentProduksjonsInfo;
}
