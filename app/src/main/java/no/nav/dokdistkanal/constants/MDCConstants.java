package no.nav.dokdistkanal.constants;

import java.util.Set;

public class MDCConstants {

	public static final String CALL_ID = "callId";
	public static final String CONSUMER_ID = "consumerId";
	public static final String USER_ID = "userId";

	public static Set<String> ALL_KEYS = Set.of(CALL_ID);

	private MDCConstants() {
	}
}
