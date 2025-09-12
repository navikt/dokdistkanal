package no.nav.dokdistkanal.consumer.serviceregistry;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Builder
public record IdentifierResource(InfoRecord infoRecord, List<ServiceRecord> serviceRecords) {

	@Builder
	public record InfoRecord(String identifier, String organizationName) {
	}

	@Builder
	public record ServiceRecord(String organisationNumber,
								String pemCertificate,
								String process,
								List<String> documentTypes,
								Service service) {
	}

	@Builder
	public record Service(ServiceIdentifier identifier,
						  String endpointUrl,
						  String serviceCode,
						  String serviceEditionCode,
						  Integer securityLevel) {

	}

	@Getter
	@RequiredArgsConstructor
	public enum ServiceIdentifier {
		DPO("DPO"),
		DPV("DPV");

		private final String name;
	}
}
