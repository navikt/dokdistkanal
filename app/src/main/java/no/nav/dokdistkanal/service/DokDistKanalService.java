package no.nav.dokdistkanal.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import org.springframework.stereotype.Service;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Slf4j
@Service
public class DokDistKanalService {
	public DokDistKanalResponse velgKanal(final String dokumentTypeId, final String personIdent) {
		return DokDistKanalResponse.builder().distribusjonsKanal("TelemarksKanalen").build();
	}
}
