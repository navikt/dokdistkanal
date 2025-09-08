package no.nav.dokdistkanal.consumer.serviceregistry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static no.nav.dokdistkanal.consumer.serviceregistry.IdentifierResource.ServiceIdentifier.DPO;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class DpoServiceRegistryService {

	public static final String AVTALTMELDING_DOCUMENT_IDENTIFICATOR = "urn:no:difi:avtalt:xsd::avtalt";
	public static final String AVTALTMELDING_PROCESS_IDENTIFIER = "urn:no:difi:profile:avtalt:avtalt:ver1.0";

	private final ServiceRegistryConsumer serviceRegistryConsumer;

	public DpoServiceRegistryService(ServiceRegistryConsumer serviceRegistryConsumer) {
		this.serviceRegistryConsumer = serviceRegistryConsumer;
	}

	public boolean containsAvtaltMeldingServiceRecord(String mottakerId) {
		final IdentifierResource identifierResource = serviceRegistryConsumer.getIdentifierResource(mottakerId, AVTALTMELDING_PROCESS_IDENTIFIER);

		if (identifierResource == null || isEmpty(identifierResource.serviceRecords())) {
			return false;
		}
		log.info("Henter mottakerinfo fra service registry for mottakerId={} og processIdentifier={}",
				mottakerId, AVTALTMELDING_PROCESS_IDENTIFIER);

		return erGyldigDpoServiceRegistry(identifierResource);
	}

	public boolean erGyldigDpoServiceRegistry(IdentifierResource identifierResource) {
		return identifierResource.serviceRecords().stream()
				.anyMatch(serviceRecord -> serviceRecord.service() != null &&
						serviceRecord.documentTypes().contains(AVTALTMELDING_DOCUMENT_IDENTIFICATOR) &&
						serviceRecord.service().identifier() == DPO);
	}

}
