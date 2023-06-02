package no.nav.dokdistkanal.consumer.altinn.serviceowner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidateRecipientResponse {
	private boolean inboxAccessible;
	private boolean canReceiveNotificationBySms;
	private boolean canReceiveNotificationByEmai;
}
