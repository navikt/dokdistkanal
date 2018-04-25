package no.nav.dokdistkanal.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DokDistKanalResponse {
	private DistribusjonKanalCode distribusjonsKanal;
}
