package no.nav.dokdistkanal.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokDistKanalResponse {
	private String distribusjonsKanal;
}
