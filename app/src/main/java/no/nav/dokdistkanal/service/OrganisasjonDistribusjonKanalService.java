package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.AltinnServiceOwnerConsumer;
import no.nav.dokdistkanal.consumer.brreg.HovedenhetResponse;
import no.nav.dokdistkanal.consumer.serviceregistry.DpoServiceRegistryService;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.springframework.stereotype.Component;

import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_KONKURS;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_SLETTET;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MANGLER_NODVENDIG_ROLLER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_ALTINN_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_INFOTRYGD_DOKUMENT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_SERVICE_REGISTRY_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_UTEN_ALTINN_INFO;
import static no.nav.dokdistkanal.service.BestemDistribusjonskanalService.createResponse;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erDokumentFraInfotrygd;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erGyldigAltinnNotifikasjonMottaker;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erOrganisasjonsnummer;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class OrganisasjonDistribusjonKanalService {

	public static final String DPO_AVTALEMELDING = "DPO_AVTALEMELDING";

	private final AltinnServiceOwnerConsumer altinnServiceOwnerConsumer;
	private final BrregEnhetsregisterService brregEnhetsregisterService;
	private final DpoServiceRegistryService dpoServiceRegistryService;
	private final DokdistkanalProperties.Dpo dpoProperties;

	public OrganisasjonDistribusjonKanalService(AltinnServiceOwnerConsumer altinnServiceOwnerConsumer,
												BrregEnhetsregisterService brregEnhetsregisterService,
												DpoServiceRegistryService dpoServiceRegistryService,
												DokdistkanalProperties dokdistkanalProperties) {
		this.altinnServiceOwnerConsumer = altinnServiceOwnerConsumer;
		this.brregEnhetsregisterService = brregEnhetsregisterService;
		this.dpoServiceRegistryService = dpoServiceRegistryService;
		this.dpoProperties = dokdistkanalProperties.getDpo();
	}

	public BestemDistribusjonskanalResponse validerOrgNrOgBestemKanal(BestemDistribusjonskanalRequest request) {
		if (!erOrganisasjonsnummer(request)) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		return organisasjon(request);
	}

	private BestemDistribusjonskanalResponse organisasjon(BestemDistribusjonskanalRequest request) {
		if (erDokumentFraInfotrygd(request.getDokumenttypeId())) {
			return createResponse(request, ORGANISASJON_MED_INFOTRYGD_DOKUMENT);
		}

		if (dpoProperties.isEnabled() && erGyldigDpoMottaker(request)) {
			return createResponse(request, ORGANISASJON_MED_SERVICE_REGISTRY_INFO);
		}

		return erGyldigDpvtMottaker(request);
	}

	private boolean erGyldigDpoMottaker(BestemDistribusjonskanalRequest request) {
		return (isNotBlank(request.getForsendelseMetadataType()) && DPO_AVTALEMELDING.equals(request.getForsendelseMetadataType()))
				&& (dpoServiceRegistryService.containsAvtaltMeldingServiceRecord(request.getMottakerId()));
	}

	private BestemDistribusjonskanalResponse erGyldigDpvtMottaker(BestemDistribusjonskanalRequest request) {

		var serviceOwnerValidRecipient = altinnServiceOwnerConsumer.isServiceOwnerValidRecipient(request.getMottakerId());

		if (!erGyldigAltinnNotifikasjonMottaker(serviceOwnerValidRecipient)) {
			return createResponse(request, ORGANISASJON_UTEN_ALTINN_INFO);
		}

		HovedenhetResponse hovedenhet = brregEnhetsregisterService.hentHovedenhet(request.getMottakerId());

		if (hovedenhet == null) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		if (hovedenhet.konkurs()) {
			return createResponse(request, ORGANISASJON_ER_KONKURS);
		}

		if (hovedenhet.slettedato() != null) {
			return createResponse(request, ORGANISASJON_ER_SLETTET);
		}

		boolean harEnhetenGyldigRolletypeForDpvt = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(hovedenhet.organisasjonsnummer());

		if (!harEnhetenGyldigRolletypeForDpvt) {
			return createResponse(request, ORGANISASJON_MANGLER_NODVENDIG_ROLLER);
		}

		return createResponse(request, ORGANISASJON_MED_ALTINN_INFO);
	}
}
