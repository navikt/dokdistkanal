package no.nav.dokdistkanal.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.ValidateRecipientResponse;
import no.nav.dokdistkanal.exceptions.functional.UgyldigInputFunctionalException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.FunctionalUtils.isEmpty;
import static no.nav.dokdistkanal.common.MottakerTypeCode.ORGANISASJON;

@Slf4j
@Component
public class DokdistkanalValidator {

	private static final Set<String> VALID_DPVT_ORGANISASJONSNUMMER = Set.of("974761076", "987926279", "912998827", "983887457");
	private static final Set<String> INFOTRYGD_DOKUMENTTYPE_ID = Set.of("000044", "000045", "000046", "000249");

	public boolean erGyldigAltinnNotifikasjonMottaker(ValidateRecipientResponse validateRecipientResponse) {
		return  validateRecipientResponse.canReceiveNotificationByEmail()
				& validateRecipientResponse.canReceiveNotificationBySms()
				& validateRecipientResponse.inboxAccessible();
	}

	public boolean isOrgNummerWithInfotrygdDokumentTypeId(DokDistKanalRequest dokDistKanalRequest) {
		return INFOTRYGD_DOKUMENTTYPE_ID.contains(dokDistKanalRequest.getDokumentTypeId());
	}

	public boolean isValidDPVTOrgNummer(DokDistKanalRequest dokDistKanalRequest) {
		return ORGANISASJON.equals(dokDistKanalRequest.getMottakerType()) && VALID_DPVT_ORGANISASJONSNUMMER.contains(dokDistKanalRequest.getMottakerId());
	}

	public static void validateInput(DokDistKanalRequest dokDistKanalRequest) {
		assertNotNullOrEmpty("dokumentTypeId", dokDistKanalRequest.getDokumentTypeId());
		assertNotNullOrEmpty("mottakerId", dokDistKanalRequest.getMottakerId());
		assertNotNullOrEmpty("mottakerType", dokDistKanalRequest.getMottakerType() == null ?
				null : dokDistKanalRequest.getMottakerType().name());
		assertNotNullOrEmpty("brukerId", dokDistKanalRequest.getBrukerId());
		assertNotNull("erArkivert", dokDistKanalRequest.getErArkivert());
		assertNotNullOrEmpty("tema", dokDistKanalRequest.getTema());
	}

	private static void assertNotNullOrEmpty(String fieldName, String value) {
		if (isEmpty(value)) {
			throw new UgyldigInputFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null eller tomt. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

	private static void assertNotNull(String fieldName, Boolean value) {
		if (value == null) {
			throw new UgyldigInputFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null. Fikk %s=%s", fieldName, fieldName, value));
		}
	}
}
