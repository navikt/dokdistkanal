package no.nav.dokdistkanal.service;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer;
import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Slf4j
@Service
public class DokDistKanalService {

	public static final Logger LOG = LoggerFactory.getLogger(DokDistKanalService.class);

	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private PersonV3Consumer personV3Consumer;
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;

	@Inject
	DokDistKanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer, PersonV3Consumer personV3Consumer, DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer, SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.personV3Consumer = personV3Consumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
	}

	public DokDistKanalResponse velgKanal(final String dokumentTypeId, final String mottakerId) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(dokumentTypeId);

		//TODO dersom det er dokumenttype som ikke skal arkiveres, skal det alltid på PRINT
		if ("INGEN".equals(dokumentTypeInfoTo.getArkivsystem())) {
			return DokDistKanalResponse.builder().distribusjonsKanal(PRINT).build();
		}
		if (mottakerId.length() != 11) {
			//Ikke personnnr
			return DokDistKanalResponse.builder().distribusjonsKanal(PRINT).build();
		} else {
			PersonV3To personTo = personV3Consumer.hentPerson(mottakerId, "VELG_KANAL");

			if (personTo == null) {
				return logAndReturn(PRINT, "Finner ikke personen i TPS");
			}

			if (personTo.getDoedsdato() != null) {
				return logAndReturn(PRINT, "Personen er død");
			}

			if (personTo.getFoedselsdato() == null) {
				return logAndReturn(PRINT, "Personens alder er ukjent");
			}

			if (LocalDate.now().minusYears(18).isBefore(personTo.getFoedselsdato())) {
				return logAndReturn(PRINT, "Personen må være minst 18 år gammel");
			}

			DigitalKontaktinformasjonTo dki = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(mottakerId + "22", "service");
			if (dki == null) {
				return logAndReturn(PRINT, "Finner ikke DKI");
			}

			if (dki.isReservasjon()) {
				return logAndReturn(PRINT, "Bruker har reservert seg");
			}
			if (dokumentTypeInfoTo.isVarslingSdp() && StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi");
			}
			if (dki.verifyAddress()) {
				return logAndReturn(SDP, "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi.");
			}
			if (StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Epostadresse og mobiltelefon - feltene er tomme");
			}

			SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(mottakerId);
			if (sikkerhetsnivaaTo == null) {
				return logAndReturn(PRINT, "Paaloggingsnivaa ikke tilgjengelig");
			}

			if (sikkerhetsnivaaTo.isHarLoggetPaaNivaa4()) {
				return logAndReturn(DITT_NAV, "Bruker har logget på med nivaa4 de siste 18 mnd");
			}

			return logAndReturn(PRINT, "Bruker har ikke logget på med nivaa4 de siste 18 mnd");
		}
	}

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode code, String reason) {

		LOG.info("BestemKanal: Sender melding til " + code.name() + ": " + reason);

		return DokDistKanalResponse.builder().distribusjonsKanal(code).build();
	}
}