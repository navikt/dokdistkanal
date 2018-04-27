package no.nav.dokdistkanal.nais.selftest.support;

/**
 * Created by T133804 on 15.08.2017.
 */
public class Ping {
	private String name;
	private String beskrivelse;
	private String address;
	private Type type;
	private Runnable pinger;

	public enum Type {
		Soap("Soap WebService"),
		Rest("Rest");

		private String beskrivelse;

		Type(String beskrivelse) {
			this.beskrivelse = beskrivelse;
		}
	}

	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
}

