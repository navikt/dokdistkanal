package no.nav.dokdistkanal.service;

import lombok.extern.slf4j.Slf4j;
import no.bekk.bekkopen.org.OrganisasjonsnummerValidator;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.ValidateRecipientResponse;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

import static no.nav.dokdistkanal.constants.MDCConstants.CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.USER_ID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.MDC.get;

@Slf4j
@Component
public class DokdistkanalValidator {

	// Fødselsnummer eller D-nummer i folkeregisteret
	private static final Pattern FOLKEREGISTERIDENT_REGEX = Pattern.compile("[0-7]\\d{10}");
	private static final String ONLY_ONES = "11111111111";

	private static final Set<String> AARSOPPGAVE_DOKUMENTTYPE_ID = Set.of("000053", "000077");
	private static final Set<String> INFOTRYGD_DOKUMENTTYPE_ID = Set.of("000044", "000045", "000046", "000249");

	public static boolean erGyldigAltinnNotifikasjonMottaker(ValidateRecipientResponse validateRecipientResponse) {
		return validateRecipientResponse.inboxAccessible() &&
			   (validateRecipientResponse.canReceiveNotificationBySms() || validateRecipientResponse.canReceiveNotificationByEmail());
	}

	public static boolean erDokumentFraInfotrygd(String dokumentTypeId) {
		return INFOTRYGD_DOKUMENTTYPE_ID.contains(dokumentTypeId);
	}

	public static boolean erOrganisasjonsnummer(BestemDistribusjonskanalRequest request) {
		return OrganisasjonsnummerValidator.isValid(request.getMottakerId());
	}

	public static boolean erIdentitetsnummer(String mottakerId) {
		return FOLKEREGISTERIDENT_REGEX.matcher(mottakerId).matches() && !ONLY_ONES.equals(mottakerId);
	}

	public static boolean erDokumentFraAarsoppgave(String dokumentTypeId) {
		return AARSOPPGAVE_DOKUMENTTYPE_ID.contains(dokumentTypeId);
	}

	public static String consumerId() {
		return isNotBlank(get(CONSUMER_ID)) ? get(CONSUMER_ID) : get(USER_ID);
	}
}
