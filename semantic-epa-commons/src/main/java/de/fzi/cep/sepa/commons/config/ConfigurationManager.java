package de.fzi.cep.sepa.commons.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.gson.JsonElement;

public class ConfigurationManager {

	public static boolean isConfigured() {
		return new File(getStreamPipesConfigFileLocation()).exists() && new File(getStreamPipesConfigFullPath()).exists();
	}
	
	public static String getStreamPipesConfigFullPath()
	{
		return getStreamPipesConfigFileLocation() + getStreamPipesConfigFilename();
	}
	
	public static String getStreamPipesConfigFileLocation()
	{
		return System.getProperty("user.home") + File.separator +".streampipes" +File.separator;
	}
	
	public static String getStreamPipesConfigFilename() 
	{
		return "streampipes-server.config";
	}

	public static WebappConfigurationSettings getWebappConfigurationFromProperties() {
		Configuration cfg = Configuration.getInstance();
		WebappConfigurationSettings settings = new WebappConfigurationSettings();
		settings.setCouchDbConnectionDbName(cfg.COUCHDB_CONNECTION_DB);
		settings.setCouchDbHost(cfg.COUCHDB_HOSTNAME);
		settings.setCouchDbMonitoringDbName(cfg.COUCHDB_MONITORING_DB);
		settings.setCouchDbPipelineDbName(cfg.COUCHDB_PIPELINE_DB);
		settings.setCouchDbPort(cfg.COUCHDB_PORT);
		settings.setCouchDbProtocol(cfg.COUCHDB_PROTOCOL);
		settings.setCouchDbUserDbName(cfg.COUCHDB_USER_DB);
		settings.setHippoUrl(cfg.HIPPO_URL);
		settings.setJmsHost(cfg.JMS_HOST);
		settings.setJmsPort(cfg.JMS_PORT);
		settings.setJmsProtocol(cfg.JMS_PROTOCOL);
		settings.setKafkaHost(cfg.KAFKA_HOST);
		settings.setKafkaPort(cfg.KAFKA_PORT);
		settings.setKafkaProtocol(cfg.KAFKA_PROTOCOL);
		settings.setPanddaUrl(cfg.PANDDA_URL);
		settings.setSesameDbName(cfg.SESAME_REPOSITORY_ID);
		settings.setSesameUrl(cfg.SESAME_URI);
		settings.setStreamStoryUrl(cfg.STREAMSTORY_URL);
		settings.setZookeeperHost(cfg.ZOOKEEPER_HOST);
		settings.setZookeeperPort(cfg.ZOOKEEPER_PORT);
		settings.setZookeeperProtocol(cfg.ZOOKEEPER_PROTOCOL);
		
		return settings;
	}
	
	public static void storeWebappConfigurationToProperties(File file, File pathToFile, WebappConfigurationSettings settings) throws IOException {
		
		Properties properties = new Properties() {
		    @Override
		    public synchronized Enumeration<Object> keys() {
		        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		    }
		};
		
		properties.put("couchDbProtocol", settings.getCouchDbProtocol());
		properties.put("couchDbHost", settings.getCouchDbHost());
		properties.put("couchDbPort", String.valueOf(settings.getCouchDbPort()));
		
		properties.put("kafkaProtocol", settings.getKafkaProtocol());
		properties.put("kafkaHost", settings.getKafkaHost());
		properties.put("kafkaPort", String.valueOf(settings.getKafkaPort()));
		
		properties.put("zookeeperProtocol", settings.getZookeeperProtocol());
		properties.put("zookeeperHost", settings.getZookeeperHost());
		properties.put("zookeeperPort", String.valueOf(settings.getZookeeperPort()));
		
		properties.put("jmsProtocol", settings.getJmsProtocol());
		properties.put("jmsHost", settings.getJmsHost());
		properties.put("jmsPort", String.valueOf(settings.getJmsPort()));
		
		properties.put("couchDbUserDbName", settings.getCouchDbUserDbName());
		properties.put("couchDbPipelineDbName", settings.getCouchDbPipelineDbName());
		properties.put("couchDbMonitoringDbName", settings.getCouchDbMonitoringDbName());
		properties.put("couchDbConnectionDbName", settings.getCouchDbConnectionDbName());
		properties.put("couchDbNotificationDbName", settings.getCouchDbConnectionDbName());
		
		properties.put("sesameUrl", settings.getSesameUrl());
		properties.put("sesameDbName", settings.getSesameDbName());
		properties.put("panddaUrl", settings.getPanddaUrl());
		properties.put("hippoUrl", settings.getHippoUrl());
		properties.put("streamStoryUrl", settings.getStreamStoryUrl());
		
		properties.put("hostname", "localhost");
		properties.put("server_url", "http://localhost");
		properties.put("tcp_server_url", "tcp://localhost");
		properties.put("tcp_server_port", "61616");
		properties.put("action_port", "8091");
		properties.put("esper_port", "8090");
		properties.put("algorithm_port", "8092");
		properties.put("sources_port", "8089");
		properties.put("webapp_port", "8080");
		properties.put("context_path", "/semantic-epa-backend");
		
		if (!pathToFile.exists()) pathToFile.mkdir();
		if (!file.exists()) file.createNewFile();
	
		properties.store(new FileWriter(file), "");
		Configuration.update();
	}

}