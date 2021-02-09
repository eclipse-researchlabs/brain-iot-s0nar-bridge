package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.ByteArrayBuffer;
import org.osgi.service.component.annotations.Component;

import com.improvingmetrics.brain.iot.demo.emitter.api.EmitterMessageDTO;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;
import fr.cea.brain.iot.sensinact.api.sica.SicaReadResponse;

@Component
@SmartBehaviourDefinition(
		consumed = {EmitterMessageDTO.class, SicaReadResponse.class},
		filter = "(timestamp=*)",
        author = "Improving Metrics",
        name = "s0nar-bridge Behaviour",
        description = "Serves as a bridge between event bus communicatiosn and sOnar server"
)
public class ComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	
	private static final Logger LOG = Logger.getLogger(ComponentImpl.class.getName());
	private final Map<String, String> SERVER_TO_DS_MAP = new HashMap<String, String>();
	private final String s0narApiUrl = "http://localhost:5004/s0nar/v1";
	private final String API_KEY = "b5ea8e13f10d46c9ae33b392bb6d74de9c24e3a91fe44bb";
	
	public ComponentImpl() {
		SERVER_TO_DS_MAP.put("cft002", "dfdb45de-8712-4a66-ba58-b5d72ac3d8c4");
	}
	
	@Override
	public void notify(BrainIoTEvent event) {
		System.out.println("*********************************************************************************************************************************");
		System.out.println("********************************************************* Event Received ********************************************************");
		System.out.println("*********************************************************************************************************************************");

		try {
			CloseableHttpClient client = HttpClients.createDefault();
			
			HttpUriRequest request = null;
			
			if (event instanceof EmitterMessageDTO) {
				request = new HttpGet("http://localhost:8888/PRUEBA_EMITTER");
			} else if (event instanceof SicaReadResponse) {
				this.digestSicaRead((SicaReadResponse)event);
			}
			
			client.execute(request);
		} catch (Exception e) {

		}
	}
	
	private void digestSicaRead(SicaReadResponse sicaRead) {
		LOG.info("Digesting SicaReadResponse " + sicaRead);
		
		try {
//			File datasetFile = this.createSicaDatasetFile(sicaRead);
//			
//			this.uploadDatasetFile(datasetFile);
			this.uploadSicaRead(sicaRead);
		} catch (Exception e) {
			LOG.severe(e.toString());
		}
	}
		
	private String getDataSetIdForSicaRead(String readerId) {
		return this.SERVER_TO_DS_MAP.get(readerId);
	}
	
	private void uploadSicaRead(SicaReadResponse sicaRead) throws ClientProtocolException, IOException {
		String dataSetId = getDataSetIdForSicaRead("cft002");
		
		boolean dataSetUploaded = false;
		
		if (dataSetId == null) {
			dataSetUploaded = this.createDataSet(sicaRead);
		} else {
			dataSetUploaded = this.updateDataSet(sicaRead, dataSetId);
		}
		
		if (dataSetUploaded) {
			
		}
	}
	
	private boolean createDataSet(SicaReadResponse sicaRead) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
//		entityBuilder.addTextBody("clave", "valor");
		
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(this.parseEvent(sicaRead, true)),
			ContentType.DEFAULT_BINARY,
			"temporaryfilename"
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.s0narApiUrl + "/dataset");
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	private byte[] parseEvent(SicaReadResponse sicaRead, boolean appendHeader) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if (appendHeader) {
			stringBuilder.append("date,cft002,cft003,dft001,eft001");
			stringBuilder.append(System.lineSeparator());
		}
		
		stringBuilder.append(sicaRead.timestamp);
		
		for (double entry : sicaRead.value) {
			stringBuilder.append(",");
			stringBuilder.append(entry);
		}
		
		stringBuilder.append(System.lineSeparator());
		
		return stringBuilder.toString().getBytes();
	}
	
	private boolean updateDataSet(SicaReadResponse sicaRead, String dataSetId) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addTextBody("name", "thename");
		entityBuilder.addTextBody("target_index", "date");
		entityBuilder.addTextBody("target_feature", "thename");
		entityBuilder.addTextBody("name", "cft002");
		entityBuilder.addTextBody("target_frequency", "10s");
		entityBuilder.addTextBody("index_schema", "%Y-%m-%d %H:%M:%S");
		entityBuilder.addTextBody("index_frequency", "10S");
				
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(this.parseEvent(sicaRead, false)),
			ContentType.DEFAULT_BINARY,
			"temporaryfilename"
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPatch(this.s0narApiUrl + "/dataset/" + dataSetId);
		request.setHeader("x-api-key", API_KEY);
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		
	}
	
	private void createModel() {
		
	}
}
