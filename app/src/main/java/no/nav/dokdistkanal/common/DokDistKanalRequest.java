package no.nav.dokdistkanal.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokDistKanalRequest {
	private String personIdent;
	private String dokumentTypeId;
}
