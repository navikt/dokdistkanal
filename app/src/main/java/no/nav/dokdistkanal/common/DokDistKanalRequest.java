package no.nav.dokdistkanal.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DokDistKanalRequest {
	private String mottakerId;
	private String dokumentTypeId;
}
