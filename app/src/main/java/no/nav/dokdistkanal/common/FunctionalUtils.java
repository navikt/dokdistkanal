package no.nav.dokdistkanal.common;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting.
 */
public final class FunctionalUtils {

	private FunctionalUtils() {
	}

	public static boolean isNotEmpty(String value) {
		return value != null && !value.isEmpty();
	}

	public static boolean isEmpty(String value) {
		return (value == null || value.isEmpty());
	}
}