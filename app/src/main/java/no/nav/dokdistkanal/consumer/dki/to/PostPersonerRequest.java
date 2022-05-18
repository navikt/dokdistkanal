package no.nav.dokdistkanal.consumer.dki.to;

import lombok.Builder;

import java.util.List;

@Builder
public class PostPersonerRequest {

	public List<String> personidenter;
}
