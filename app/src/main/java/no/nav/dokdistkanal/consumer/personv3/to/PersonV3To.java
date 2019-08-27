package no.nav.dokdistkanal.consumer.personv3.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonV3To implements Serializable {
	static final long serialVersionUID = 1L;
	private LocalDate doedsdato;
	private LocalDate foedselsdato;
}
