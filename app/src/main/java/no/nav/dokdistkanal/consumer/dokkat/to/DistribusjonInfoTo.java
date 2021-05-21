package no.nav.dokdistkanal.consumer.dokkat.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribusjonInfoTo {
	private String predefinertDistKanal;
	private List<DistribusjonVarselTo> distribusjonVarsels = new ArrayList();
}
