package no.nav.dokdistkanal.metrics;

/**
 * @author Jakob A. Libak, NAV.
 */
public class PrometheusLabels {

	public static final String LABEL_TECHNICAL_EXCEPTION = "technical";
	public static final String LABEL_FUNCTIONAL_EXCEPTION = "functional";
	public static final String LABEL_SECURITY_EXCEPTION = "security";

	public static final String LABEL_NAME = "name";
	public static final String LABEL_EXCEPTION_NAME = "exception_name";
	public static final String LABEL_PROCESS = "process";
	public static final String LABEL_PROCESS_CODE = "process_called";
	public static final String LABEL_TYPE = "type";
	public static final String LABEL_PROCESS_CALLED = "process_title";
	public static final String LABEL_CONSUMER_ID = "consumer_name";

	public static final String LABEL_EVENT = "event";
	public static final String LABEL_ERROR_TYPE = "error_type";

	public static final String LABEL_DOKDIST="DokDistKanal";

	//Cache
	public static final String CACHE_COUNTER = "cacheCounter";
	public static final String CACHE_MISS = "cacheMiss";
	public static final String CACHE_TOTAL = "cacheTotal";
	public static final String CACHE_ERROR = "cacheError";
	public static final String REDIS_CACHE = "redisCache";


	public static final String MOTTAKERTYPE = "mottakerType";
	public static final String PLUGIN = "plugin";
	public static final String CONTROLLER = "controller";
	public static final String REST = "Rest";
	public static final String PERSONV3 = "personV3";
	public static final String DIGITALKONTAKTINFORMASJONV1 = "digitalKontaktInformasjonV1";
	public static final String SIKKERHETSNIVAAV1 = "SikkerhetsnivaaV1";
	public static final String DOKUMENTTYPEINFOV4 = "DokumenttypeInfoV4";
	public static final String RECEIVED = "received";
	public static final String PROCESSED_OK = "processed_ok";

}
