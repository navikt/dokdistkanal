package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.serviceregistry.DpoServiceRegistryService;
import no.nav.dokdistkanal.domain.Enhetsopplysninger;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Hovedenhet;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Underenhet;
import no.nav.dokdistkanal.exceptions.functional.EnhetSlettetException;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.springframework.stereotype.Component;

import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_KONKURS;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MANGLER_NODVENDIG_ROLLER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_ALTINN_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_INFOTRYGD_DOKUMENT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_SERVICE_REGISTRY_INFO;
import static no.nav.dokdistkanal.service.BestemDistribusjonskanalService.createResponse;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erDokumentFraInfotrygd;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erOrganisasjonsnummer;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class OrganisasjonDistribusjonKanalService {

	public static final String DPO_AVTALEMELDING = "DPO_AVTALEMELDING";

	private final BrregEnhetsregisterService brregEnhetsregisterService;
	private final DpoServiceRegistryService dpoServiceRegistryService;
	private final DokdistkanalProperties.Dpo dpoProperties;

	public OrganisasjonDistribusjonKanalService(BrregEnhetsregisterService brregEnhetsregisterService,
												DpoServiceRegistryService dpoServiceRegistryService,
												DokdistkanalProperties dokdistkanalProperties) {
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

		return bestemKanalForOrganisasjon(request);
	}

	private boolean erGyldigDpoMottaker(BestemDistribusjonskanalRequest request) {
		return (isNotBlank(request.getForsendelseMetadataType()) && DPO_AVTALEMELDING.equals(request.getForsendelseMetadataType()))
				&& (dpoServiceRegistryService.containsAvtaltMeldingServiceRecord(request.getMottakerId()));
	}

	private BestemDistribusjonskanalResponse bestemKanalForOrganisasjon(BestemDistribusjonskanalRequest request) {
		Enhetsopplysninger enhetsopplysninger = brregEnhetsregisterService.hentEnhetsopplysninger(request.getMottakerId());

		if (enhetsopplysninger.erUnderenhetSlettet()) {
			Underenhet underenhet = enhetsopplysninger.underenhet();
			throw new EnhetSlettetException(underenhet.organisasjonsnummer(), underenhet.slettedato());
		}

		if (!enhetsopplysninger.harHovedenhet()) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		if (enhetsopplysninger.erHovedenhetKonkurs()) {
			return createResponse(request, ORGANISASJON_ER_KONKURS);
		}

		if (enhetsopplysninger.erHovedenhetSlettet()) {
			Hovedenhet hovedenhet = enhetsopplysninger.hovedenhet();
			throw new EnhetSlettetException(hovedenhet.organisasjonsnummer(), hovedenhet.slettedato());
		}

		boolean harEnhetenGyldigRolletypeForDpvt = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(enhetsopplysninger.hovedenhet().organisasjonsnummer());

		if (!harEnhetenGyldigRolletypeForDpvt) {
			return createResponse(request, ORGANISASJON_MANGLER_NODVENDIG_ROLLER);
		}

		return createResponse(request, ORGANISASJON_MED_ALTINN_INFO);
	}
}
