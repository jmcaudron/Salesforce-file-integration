package com.jmc.force.login;

import java.io.File;
import java.io.IOException;

import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.LoginResult;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
//import com.sforce.ws.shade.org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper;

//import com.sforce.soap.metadata.MetadataConnection;

/**
 * Login utility.
 */
public class loginUtil {
	private static Logger logger = Logger.getLogger(loginUtil.class);
   
	private static  loginInfo logInfo ;
	
    public static class loginInfo  {
    	public String username;
    	public String password;
    	public String token;
    	public String apiVersion;
    	public String apiService;
    	public String url;
    
    	public loginInfo() {
    		super();
    		username=password=apiVersion=url=apiService=token="";
    	}
    	
    	
    	public loginInfo (String userame, String password,String token, String apiversion,String apiservice, String url) {
    		this.username = userame;
    		this.apiVersion = apiversion;
    		this.apiService = apiservice;
    		this.password = password;
    		this.token = token;
    		this.url = url;
    	}
    }
    
    private  LoginResult loginToSalesforce(
            final String username,
            final String password,
            final String loginUrl) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setAuthEndpoint(loginUrl);
        config.setServiceEndpoint(loginUrl);
        config.setManualLogin(true);
        config.setPrettyPrintXml(true);
        return (new PartnerConnection(config)).login(username, password);
    }
    
    private  void readLoginInfo(String configFile) throws JsonParseException, JsonMappingException, IOException {
    	if (configFile == "") 
    		configFile= "conf/loginInfo.json";
       	ObjectMapper mapper = new ObjectMapper();
	  	
   		logInfo= mapper.readValue(new File(configFile), loginInfo.class);
    }
    
    private ConnectorConfig getConnectorConfig(String configFile) throws JsonParseException, JsonMappingException, IOException, ConnectionException {
    	final ConnectorConfig config = new ConnectorConfig();
    	
    	readLoginInfo(configFile);  
    	logger.info("Connection url : 		"  +logInfo.url);
    	logger.info("Connection Usermane : 	"  +logInfo.username);

        LoginResult loginResult = loginToSalesforce(logInfo.username, logInfo.password+ logInfo.token, logInfo.url);
        config.setServiceEndpoint(loginResult.getMetadataServerUrl());
        config.setSessionId(loginResult.getSessionId());
	        //partnerConfig.setAuthEndpoint(authPoint);
	        //partnerConfig.setManualLogin(true);
	        // Creating the connection automatically handles login and stores
	        // the session in partnerConfig
	        //new PartnerConnection(partnerConfig);
	        // When PartnerConnection is instantiated, a login is implicitly
	        // executed and, if successful,
	        // a valid session is stored in the ConnectorConfig instance.
	        // Use this key to initialize a BulkConnection:

	     // The endpoint for the Bulk API service is the same as for the normal
	     // SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'
	     String soapEndpoint = config.getServiceEndpoint();

	     String restEndpoint = soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/"))+  logInfo.apiService + logInfo.apiVersion;
	     config.setRestEndpoint(restEndpoint);
        // This should only be false when doing debugging.
        config.setCompression(true);
        //config.setCompression(true); //JMC error sur job
        // config.setCompression(false);
        // Set this to true to see HTTP requests and responses on stdout
        config.setTraceMessage(false);  
        //config.setTraceMessage(true);
    	return config;
    }

    public String getAPIVersion() {
    	return logInfo.apiVersion;
    }
   /* public MetadataConnection getMetadataConnection(String configFile) 
    		throws Exception, JsonMappingException, IOException 
    {
		final ConnectorConfig config ;
		config = getConnectorConfig(configFile);
		MetadataConnection connection = new MetadataConnection(config);
		return connection;
	}   */ 
    public BulkConnection getBulkConnection(String configFile) 
    		throws Exception, JsonMappingException, IOException 
	           {
	    	final ConnectorConfig config ;
	    	config = getConnectorConfig(configFile);
	    	BulkConnection connection = new BulkConnection(config);
		    return connection;
	    }
}
