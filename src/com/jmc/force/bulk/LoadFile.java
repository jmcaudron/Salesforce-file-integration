package com.jmc.force.bulk;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.jmc.force.login.loginUtil;

//import com.jmc.param.ParamManagement;
//import com.jmc.param.ParamManagement.importConfig;

//import com.jmc.param.ParamManagement;
//import com.jmc.param.ParamManagement.importConfig;
//import com.jmc.param.ParamManagement.parameters;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
//import com.sforce.async.CSVReader;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
   
public class LoadFile {


	private static Logger logger = Logger.getLogger(LoadFile.class);

    public static void main(String[] args)
      throws Exception {
        LoadFile example = new LoadFile();
        PropertyConfigurator.configure("./conf/log4j.properties");
        
        example.runFromConfig();
    }

    /**
     * Creates a Bulk API job and uploads batches for a CSV file.
     * @throws Exception 
     */
    public void runFromConfig()
            throws Exception {
    	//String outputDirectory;
    	 	
    	logger.info("Start");
    	//System.setProperty( "file.encoding", "UTF-8" );

    	logger.info("Load Job");
    	
    	com.jmc.param.ParamManagement pm = new com.jmc.param.ParamManagement("conf/jobsLoad.json"); 
    	//parameters paramlst = pm.params;
    	
    	//ObjectMapper mapper = new ObjectMapper();
    	//mapper.writeValue(new File("params.json"), pm.params);


    	//parameters param= pm.readLoginInfo("");
        //BulkConnection connection = Login.getBulkConnection(username, password,authendpoint,apiVersion);
    	
    	logger.info("Connect Salesforce");
    	loginUtil lu = new loginUtil();
    	BulkConnection connection = lu.getBulkConnection("");
    	//outputDirectory=lu.getOutputDirectory();
        logger.info("connection:"+ connection.getConfig().getSessionId());
        logger.info("restEndpoint : 		"  +connection.getConfig().getRestEndpoint());
        for (com.jmc.param.ParamManagement.importConfig ic : pm.params.importConfigs) {
        	JobInfo job = createJob(ic.object,ic.operation,ic.externalId,ic.mode,ic.contentType, connection);
            List<BatchInfo> batchInfoList;
            boolean withHeader;
            
            withHeader = true;
            if (ic.operation != com.sforce.async.OperationEnum.query ||ic.operation != com.sforce.async.OperationEnum.queryAll) {
            	withHeader = false;
            }
 
        	if (ic.specFile != "")
            	addSpec(connection, job, ic.specFile);

            	batchInfoList = createBatchesFromCSVFile(connection, job,ic.dataFile,pm.params.maxRowsPerBatch,pm.params.maxBytesPerBatch, withHeader);

            closeJob(connection, job.getId());
            awaitCompletion(connection, job, batchInfoList);
            checkResults(connection, job, batchInfoList,ic.dataFile );
        }
        logger.info("end");

    }


	/**
     * Gets the results of the operation and checks for errors.
     */
    private void checkResults(BulkConnection connection, JobInfo job,
              List<BatchInfo> batchInfoList, String inputFileName)
            throws AsyncApiException, IOException {
        // batchInfoList was populated when batches were created and submitted
    	job = connection.getJobStatus(job.getId());
    	logger.info("Job STATUS:"+ job.getNumberRecordsFailed()+"/"+ job.getNumberRecordsProcessed()+" in "+ job.getTotalProcessingTime());
    	logger.debug("Job STATUS:"+job);
  
		File outFile ;
		FileWriter frOut ;
		BufferedWriter brOut ;

		if (job.getOperation()==OperationEnum.query || job.getOperation()==OperationEnum.queryAll) {
			for (BatchInfo bi : batchInfoList) {
				outFile = new File("log/"+job.getObject()+"-"+job.getId()+ bi.getId()+".csv");
				frOut = new FileWriter(outFile);
				brOut  = new BufferedWriter(frOut);
				
				String[] queryResults = null;
				queryResults = connection.getQueryResultList(job.getId(), bi.getId()).getResult(); 
				for (String resultId : queryResults) {
					logger.info("resultId:"+resultId);
					InputStream inResult =connection.getQueryResultStream(job.getId(), bi.getId(),resultId);
					BufferedReader bfrResult = new BufferedReader(new InputStreamReader(inResult, "UTF8"));
					String nextLineResult;       
		            while ( (nextLineResult = bfrResult.readLine()) != null) {
		            	 brOut.write(nextLineResult+"\n");
		            }
		            bfrResult.close();
				}

	            brOut.close();
				
			}
			
		}
		else {
			outFile = new File("log/"+job.getObject()+"-"+job.getId()+".csv");
			frOut = new FileWriter(outFile);
			brOut  = new BufferedWriter(frOut);
		
	    	for (BatchInfo b : batchInfoList) {
	        	   
	             InputStream inInput =new FileInputStream(inputFileName);
	             //InputStream inInput =connection.getBatchRequestInputStream(job.getId(), b.getId());
	             InputStream inResult =connection.getBatchResultStream(job.getId(), b.getId());
	             
	             BufferedReader bfrInput = new BufferedReader(new InputStreamReader(inInput, "UTF8"));
	             BufferedReader bfrResult = new BufferedReader(new InputStreamReader(inResult, "UTF8"));
	
	             
	             String nextLineInput;
	             String nextLineResult;       
	             while (((nextLineInput = bfrInput.readLine()) != null) && ((nextLineResult = bfrResult.readLine()) != null)) {
	            	 brOut.write(nextLineInput+","+nextLineResult+"\n");
	             }
	             bfrInput.close();
	             bfrResult.close();
	             
	    	}
	        brOut.close();
		}
            
        	/*
        	CSVReader rdrInput =
                    new CSVReader(connection.getBatchRequestInputStream(job.getId(), b.getId()));
        	CSVReader rdrResult =
              new CSVReader(connection.getBatchResultStream(job.getId(), b.getId()));
        	
            List<String> resultHeader = rdrResult.nextRecord();
            List<String> inputHeader = rdrInput.nextRecord();
            List<String> logHeader = inputHeader;
            logHeader.addAll(resultHeader);
            
            
            int resultCols = resultHeader.size();
            int inputCols = resultHeader.size();
            
            logger.debug("resultHeader ="+resultHeader.toString());
            List<String> row;
            while ((row = rdrResult.nextRecord()) != null) {
            	logger.debug("row ="+row.toString());
                Map<String, String> resultInfo = new HashMap<String, String>();
                
                for (int i = 0; i < resultCols; i++) {
                    resultInfo.put(resultHeader.get(i), row.get(i));
                }
                boolean success = Boolean.valueOf(resultInfo.get("Success"));
                boolean created = Boolean.valueOf(resultInfo.get("Created"));
                String id = resultInfo.get("Id");
                String error = resultInfo.get("Error");
                if (success && created) {
                    logger.info("Created row with id " + id );
                } else if (success) {
                	logger.info("Updated row with id " + id );
                } else {
                	logger.error("Error detected : " + error);
                }
            }*/
        
    	
    }



    private void closeJob(BulkConnection connection, String jobId)
          throws AsyncApiException {
        JobInfo job = new JobInfo();
        job.setId(jobId);
        job.setState(JobStateEnum.Closed);
        connection.updateJob(job);
    }



    /**
     * Wait for a job to complete by polling the Bulk API.
     * 
     * @param connection
     *            BulkConnection used to check results.
     * @param job
     *            The job awaiting completion.
     * @param batchInfoList
     *            List of batches for this job.
     * @throws AsyncApiException
     */
    private void awaitCompletion(BulkConnection connection, JobInfo job,
          List<BatchInfo> batchInfoList)
            throws AsyncApiException {
        long sleepTime = 0L;
        Set<String> incomplete = new HashSet<String>();
        for (BatchInfo bi : batchInfoList) {
            incomplete.add(bi.getId());
        }
        while (!incomplete.isEmpty()) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
            logger.info("Awaiting results..." + incomplete.size());
            sleepTime = 10000L;
            BatchInfo[] statusList =
              connection.getBatchInfoList(job.getId()).getBatchInfo();
            for (BatchInfo b : statusList) {
                if (b.getState() == BatchStateEnum.Completed
                  || b.getState() == BatchStateEnum.Failed) {
                    if (incomplete.remove(b.getId())) {
                    	logger.info("BATCH STATUS:"+ b.getNumberRecordsFailed()+"/"+ b.getNumberRecordsProcessed()+" in "+ b.getTotalProcessingTime());
                    }
                }
            }
        }
        
    }



    /**
     * Create a new job using the Bulk API.
     * 
     * @param sobjectType
     *            The object type being loaded, such as "Account"
     * @param connection
     *            BulkConnection used to create the new job.
     * @return The JobInfo for the new job.
     * @throws AsyncApiException
     */
    private JobInfo createJob(String sobjectType,OperationEnum operation,String externalIdFieldName,ConcurrencyMode mode,ContentType contentType,  BulkConnection connection)
          throws AsyncApiException {
        JobInfo jobr,job = new JobInfo();
        
        logger.info("create Job for object <"+sobjectType+"> with operation <"+operation+"> externalID = <"+externalIdFieldName+">");
        
        job.setObject(sobjectType);
        job.setOperation(operation);
        job.setContentType(contentType);
        job.setConcurrencyMode(mode);
        job.setExternalIdFieldName(externalIdFieldName); //Doesn't work
        jobr = connection.createJob(job);
       

        
        //System.out.println(jobr);
        return jobr;
    }

    

    /**
     * Create the BulkConnection used to call Bulk API operations.
     */
   /* private BulkConnection getBulkConnection(String userName, String passWord, String authPoint)
          throws ConnectionException, AsyncApiException {
        ConnectorConfig partnerConfig = new ConnectorConfig();
        partnerConfig.setUsername(userName);
        partnerConfig.setPassword(passWord);
        partnerConfig.setAuthEndpoint(authPoint);
        //partnerConfig.setManualLogin(true);
        // Creating the connection automatically handles login and stores
        // the session in partnerConfig
        new PartnerConnection(partnerConfig);
        // When PartnerConnection is instantiated, a login is implicitly
        // executed and, if successful,
        // a valid session is stored in the ConnectorConfig instance.
        // Use this key to initialize a BulkConnection:
        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(partnerConfig.getSessionId());
        // The endpoint for the Bulk API service is the same as for the normal
        // SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'
        String soapEndpoint = partnerConfig.getServiceEndpoint();

        String restEndpoint = soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/"))+ "async/" + apiVersion;
        config.setRestEndpoint(restEndpoint);
        // This should only be false when doing debugging.
        config.setCompression(true);
        //config.setCompression(true); //JMC error sur job
        // config.setCompression(false);
        // Set this to true to see HTTP requests and responses on stdout
        //config.setTraceMessage(false);
        BulkConnection connection = new BulkConnection(config);
        return connection;
    }*/



    /**
     * Create and upload batches using a CSV file.
     * The file into the appropriate size batch files.
     * 
     * @param connection
     *            Connection to use for creating batches
     * @param jobInfo
     *            Job associated with new batches
     * @param csvFileName
     *            The source file for batch data
     */
    private List<BatchInfo> createBatchesFromCSVFile(BulkConnection connection,
          JobInfo jobInfo, String csvFileName,int maxRowsPerBatch, int maxBytesPerBatch, boolean withHeader )
            throws IOException, AsyncApiException {
    	logger.info("createBatchesFromCSVFile <"+csvFileName+">");
    	
        List<BatchInfo> batchInfos = new ArrayList<BatchInfo>();
        BufferedReader rdr = new BufferedReader(
            new InputStreamReader(new FileInputStream(csvFileName))
        );
        // read the CSV header row
        byte[] headerBytes ="".getBytes("UTF-8");
        if (withHeader) 
        	headerBytes= (rdr.readLine() + "\n").getBytes("UTF-8");
        int headerBytesLength = headerBytes.length;
        File tmpFile = File.createTempFile("bulkAPIInsertJMC", ".csv");

        // Split the CSV file into multiple batches
        try {
            FileOutputStream tmpOut = new FileOutputStream(tmpFile);
            //int maxBytesPerBatch = 10000000; // 10 million bytes per batch
            //int maxRowsPerBatch = 10000; // 10 thousand rows per batch
            int currentBytes = 0;
            int currentLines = 0;
            String nextLine;
            while ((nextLine = rdr.readLine()) != null) {
                byte[] bytes = (nextLine + "\n").getBytes("UTF-8");
                // Create a new batch when our batch size limit is reached
                if (currentBytes + bytes.length > maxBytesPerBatch
                  || currentLines > maxRowsPerBatch) {
                    createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
                    currentBytes = 0;
                    currentLines = 0;
                }
                if (currentBytes == 0) {
                    tmpOut = new FileOutputStream(tmpFile);
                    if (withHeader)
                    	tmpOut.write(headerBytes);
                    currentBytes = headerBytesLength;
                    currentLines = 1;
                }
                tmpOut.write(bytes);
                currentBytes += bytes.length;
                currentLines++;
                
            }
            // Finished processing all rows
            // Create a final batch for any remaining data
            if (currentLines > 1) {
                createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
            }
        } finally {
            tmpFile.delete();
            rdr.close();
        }
        return batchInfos;
    }

    /**
     * Create a batch by uploading the contents of the file.
     * This closes the output stream.
     * 
     * @param tmpOut
     *            The output stream used to write the CSV data for a single batch.
     * @param tmpFile
     *            The file associated with the above stream.
     * @param batchInfos
     *            The batch info for the newly created batch is added to this list.
     * @param connection
     *            The BulkConnection used to create the new batch.
     * @param jobInfo
     *            The JobInfo associated with the new batch.
     */
    private void createBatch(FileOutputStream tmpOut, File tmpFile,
      List<BatchInfo> batchInfos, BulkConnection connection, JobInfo jobInfo)
              throws IOException, AsyncApiException {
    	logger.info("createBatch <"+tmpFile.getName()+">");
        tmpOut.flush();
        tmpOut.close();
        FileInputStream tmpInputStream = new FileInputStream(tmpFile);
        //InputStreamReader tmpInputStream = new InputStreamReader(new FileInputStream(tmpFile), "UTF8");
        try {
        	
            BatchInfo batchInfo =
            connection.createBatchFromForeignCsvStream(jobInfo, tmpInputStream,"utf8");
            //connection.createBatchFromStream(jobInfo, tmpInputStream);
            
            //System.out.println(batchInfo);
            batchInfos.add(batchInfo);
        } catch(Exception e) {
        	throw e;
        }
        finally {
            tmpInputStream.close();
        }
    }

    /**
		addspec
     */
    private void addSpec(BulkConnection connection, JobInfo job, String fileSpec)
              throws IOException, AsyncApiException {
    	File spec = new File (fileSpec);
    	FileInputStream specStream = new FileInputStream(spec);
    	logger.info("Add for object <"+fileSpec+">");
	  	try{
	        
	    	    // BulkConnection, JobInfo, InputStream
	    	    connection.createTransformationSpecFromStream(job, specStream);
	    	} finally{
    	    specStream.close();
    	}

    }

    		 
/*	public static InputStream toUTF8(InputStream input, String inputCharsetName)
	throws UnsupportedEncodingException, IOException {
	    int size = 0;
	    byte[] data = new byte[0];
	    byte[] buff = new byte[2048];
	    int nbRead = 0;
	    try {
	        while((nbRead = input.read(buff)) > 0) {
	            String sBuff = inputCharsetName == null ? new String(buff) : new String(buff, inputCharsetName);
	            size += nbRead;
	            byte[] temp = new byte[size];
	            System.arraycopy(data, 0, temp, 0, data.length);
	            System.arraycopy(sBuff.getBytes("UTF8"), 0, temp, data.length, nbRead);
	            data = temp;
	        }
	    } finally {
	        input.close();
	    }
	    return new ByteArrayInputStream(data);
	}*/
    /*private BatchInfo createBatchFromStreamImpl(BulkConnection connection ,JobInfo jobInfo, InputStream input, boolean isZip)
            throws AsyncApiException {
        try {
            String endpoint = connection.getConfig().getRestEndpoint();
            Transport transport = connection.getConfig().createTransport();
            endpoint = endpoint + "job/" + jobInfo.getId() + "/batch";
            String contentType = "CSV";
            HashMap<String, String> httpHeaders ;//= getHeaders(contentType);
            // TODO do we want to allow the zip content to be gzipped
            boolean allowZipToBeGzipped = false;
            OutputStream out = transport.connect(endpoint, httpHeaders, allowZipToBeGzipped || !isZip);

            FileUtil.copy(input, out);

            InputStream result = transport.getContent();
            //if (!transport.isSuccessful()) parseAndThrowException(result, jobInfo.getContentType());
            //xml/json content type
            //if (jobInfo.getContentType() == ContentType.JSON || jobInfo.getContentType() == ContentType.ZIP_JSON)
            //    return deserializeJsonToObject(result, BatchInfo.class);

            return loadBatchInfo(result);

        } catch (IOException e) {
            throw new AsyncApiException("Failed to create batch", AsyncExceptionCode.ClientInputError, e);
        } catch (PullParserException e) {
            throw new AsyncApiException("Failed to create batch", AsyncExceptionCode.ClientInputError, e);
        } catch (ConnectionException e) {
            throw new AsyncApiException("Failed to create batch", AsyncExceptionCode.ClientInputError, e);
        }
    }
    static BatchInfo loadBatchInfo(InputStream in) throws PullParserException, IOException, ConnectionException {
        BatchInfo info = new BatchInfo();
        XmlInputStream xin = new XmlInputStream();
        xin.setInput(in, "UTF-8");
        info.load(xin, BulkConnection.typeMapper);
        return info;
    }*/
}