package no.nav.dokdistkanal.consumer.dokmet.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistribusjonVarselTo {
	private String varselForDistribusjonKanal;
}
