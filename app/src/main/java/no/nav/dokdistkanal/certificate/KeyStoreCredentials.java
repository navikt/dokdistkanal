package no.nav.dokdistkanal.certificate;

import java.util.StringJoiner;

record KeyStoreCredentials(String alias, String password, String type) {
	@Override
	public String toString() {
		return new StringJoiner(", ", KeyStoreCredentials.class.getSimpleName() + "[", "]")
				.add("alias='" + alias + "'")
				.add("password='*****'")
				.add("type='" + type + "'")
				.toString();
	}
}
