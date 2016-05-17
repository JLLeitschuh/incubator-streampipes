package de.fzi.proasense.hella.config;

import de.fzi.cep.sepa.commons.config.ClientConfiguration;

public class SourcesConfig {

	public final static String serverUrl;
	public final static String iconBaseUrl;
	public final static String topicPrefixDdm;
	public final static String topicPrefixRam;
	
	static {
		serverUrl = ClientConfiguration.INSTANCE.getWebappUrl();
		iconBaseUrl = ClientConfiguration.INSTANCE.getWebappUrl() +"/semantic-epa-backend/img";
		topicPrefixDdm = "SEPA.SEP.DDM.";
		topicPrefixRam = "SEPA.SEP.Ram";
	}
}