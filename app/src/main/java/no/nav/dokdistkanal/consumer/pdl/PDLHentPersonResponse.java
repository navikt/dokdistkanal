package no.nav.dokdistkanal.consumer.pdl;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PDLHentPersonResponse {

    private Foedsel foedsel;
    private Doedsfall doedsfall;
    private PdlError pdlError;

    @Data
    private static class Foedsel {
        private LocalDateTime foedselsdato;
        private Integer foedselsaar;

    }

    @Data
    private static class Doedsfall {
        private LocalDateTime doedsdato;
    }

    @Data
    static class PdlError {
        private String message;
        private PdlErrorExtensionTo extensions;
    }

    @Data
    static class PdlErrorExtensionTo {
        private String code;
        private String classification;
    }
}
