package no.nav.dokdistkanal.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DokDistKanalRequest {
	private String mottakerId;
	private String dokumentTypeId;
}
