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
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Slf4j
@Service
public class DokDistKanalService {

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

	public DokDistKanalResponse velgKanal(final String dokumentTypeId, final String personIdent) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(dokumentTypeId);
		//TODO dersom det er dokumenttype som ikke skal arkiveres, skal det alltid på PRINT
		if (dokumentTypeInfoTo != null) {
			return DokDistKanalResponse.builder().distribusjonsKanal(PRINT).build();
		} else {
			PersonV3To personTo = personV3Consumer.hentPerson(personIdent, "VELG_KANAL", "service");

			if (personTo == null) {
				return logAndReturn(PRINT, "Missing in TPS");
			}

			if (personTo.getDoedsdato() != null) {
				return logAndReturn(PRINT, "Person deceased");
			}

			if (personTo.getFoedselsdato() == null) {
				return logAndReturn(PRINT, "Persons birth date is unknown");
			}

			if (LocalDate.now().minusYears(18).isBefore(personTo.getFoedselsdato())) {
				return logAndReturn(PRINT, "Person is under 18 years old");
			}

			DigitalKontaktinformasjonTo dki = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(personIdent, "service");
			if (dki == null) {
				return logAndReturn(PRINT, "Missing DKI");
			}

			if (dki.isReservasjon()) {
				return logAndReturn(PRINT, "Is reserved");
			}
//			if (varslingSDP && StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
			if (StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Varsling is enabled while email and cellphone is empty");
			}
			if (dki.verifyAddress()) {
				return logAndReturn(SDP, "Sertifikat, LeverandørAddresse and BrukerAdresse is populated.");
			}
			if (StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Email and cellphone is empty");
			}

			SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(personIdent);
			if (sikkerhetsnivaaTo == null) {
				return logAndReturn(PRINT, "Paaloggingsnivaa not available");
			}

			if (sikkerhetsnivaaTo.isHarLoggetPaaNivaa4()) {
				return logAndReturn(DITT_NAV, "User has used nivaa4 in the last 18 months");
			}

			return logAndReturn(PRINT, "User has not used nivaa4 in the last 18 months");
		}
	}

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode code, String reason) {

		log.info("BestemKanal: Sender melding til  " + code.name() + ": " + reason);

		return DokDistKanalResponse.builder().distribusjonsKanal(code).build();
	}
}