package com.jmc.param;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.sforce.async.OperationEnum;


public class ParamManagement {
	public static class importConfig {
		public String object;
		public String dataFile;
		public String specFile;
		public OperationEnum operation;
		public String externalId;
		
		public importConfig() {
			this.dataFile="datafile";
			this.object="object";
			this.specFile="specfile";
			this.operation = OperationEnum.insert;
			this.externalId = "Id";
		}	
	}
	
	public static  class parameters  {
		 public List<importConfig> importConfigs;
		 public parameters() {
			importConfigs = new ArrayList<importConfig>();
		}
	}

	
	public parameters params ;
	
	public ParamManagement() throws JsonParseException, JsonMappingException, IOException {
		super();
		loadParameters("");
	}

	public ParamManagement(String configFile) throws JsonParseException, JsonMappingException, IOException  {
		super();
		loadParameters(configFile);
	}
	
	private void loadParameters(String configFile) throws JsonParseException, JsonMappingException, IOException {
		if (configFile == "") 
	   		configFile= "objects.json";
	    ObjectMapper mapper = new ObjectMapper();
	    File config =new File(configFile);  	
	    if (config.exists()) { 	
	    	params= mapper.readValue(new File(configFile), parameters.class);
	    } else
	    	params= new parameters();
		
	}		   

	public void initTest (Integer count) {

		importConfig c = new importConfig();
		params = new parameters();
		params.importConfigs.add(c);
		params.importConfigs.add(c);

		ObjectMapper mapper = new ObjectMapper();
		try {
			//mapper.writeValue(new File("param.json"), c);
			mapper.writeValue(new File("params.json"), params);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
