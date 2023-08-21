package no.nav.dokdistkanal.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.ValidateRecipientResponse;
import no.nav.dokdistkanal.exceptions.functional.UgyldigInputFunctionalException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.MottakerTypeCode.ORGANISASJON;
import static no.nav.dokdistkanal.constants.MDCConstants.CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.USER_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.MDC.get;

@Slf4j
@Component
public class DokdistkanalValidator {

	// Fødselsnummer eller D-nummer i folkeregisteret
	private static final Pattern FOLKEREGISTERIDENT_REGEX = Pattern.compile("[0-7]\\d{10}");
	private static final String ONLY_ONES = "11111111111";

	private static final Set<String> AARSOPPGAVE_DOKUMENTTYPE_ID = Set.of("000053", "000077");
	private static final Set<String> VALID_DPVT_ORGANISASJONSNUMMER = Set.of("974761076", "987926279", "912998827", "983887457", "914760011", "875432702", "971574909", "985569258");
	private static final Set<String> INFOTRYGD_DOKUMENTTYPE_ID = Set.of("000044", "000045", "000046", "000249");

	public static boolean erGyldigAltinnNotifikasjonMottaker(ValidateRecipientResponse validateRecipientResponse) {
		return validateRecipientResponse.inboxAccessible() &
			   (validateRecipientResponse.canReceiveNotificationBySms() || validateRecipientResponse.canReceiveNotificationByEmail());
	}

	public static boolean isOrgNummerWithInfotrygdDokumentTypeId(DokDistKanalRequest dokDistKanalRequest) {
		return INFOTRYGD_DOKUMENTTYPE_ID.contains(dokDistKanalRequest.getDokumentTypeId());
	}

	public static boolean isValidDPVTOrgNummer(DokDistKanalRequest dokDistKanalRequest) {
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

	public static boolean isFolkeregisterident(DokDistKanalRequest dokDistKanalRequest) {
		return FOLKEREGISTERIDENT_REGEX.matcher(dokDistKanalRequest.getMottakerId()).matches() && !ONLY_ONES.equals(dokDistKanalRequest.getMottakerId());
	}

	public static boolean isDokumentTypeIdUsedForAarsoppgave(String dokumentTypeId) {
		return AARSOPPGAVE_DOKUMENTTYPE_ID.contains(dokumentTypeId);
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

	public static String consumerId() {
		return isNotBlank(get(CONSUMER_ID)) ? get(CONSUMER_ID) : get(USER_ID);
	}
}
