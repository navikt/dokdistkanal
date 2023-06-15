package no.nav.dokdistkanal.consumer.altinn.serviceowner;

public record ValidateRecipientResponse(boolean canReceiveNotificationByEmail,
									   boolean inboxAccessible,
									   boolean canReceiveNotificationBySms
) {
}
