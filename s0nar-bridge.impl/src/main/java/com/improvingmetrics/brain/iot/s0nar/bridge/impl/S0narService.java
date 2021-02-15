package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

public class S0narService {
	
	private static final Logger LOG = Logger.getLogger(S0narService.class.getName());
	
	private String apiUrl;
	private String apiKey;
	
	public S0narService(String apiUrl, String apiKey) {
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
	}
	
	public boolean createDataSet(byte[] dataSetData, String feature) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addTextBody("name", "thename");
		entityBuilder.addTextBody("target_index", "date");
		entityBuilder.addTextBody("target_feature", feature);
		entityBuilder.addTextBody("name", "cft002");
		entityBuilder.addTextBody("target_frequency", "10s");
		entityBuilder.addTextBody("index_schema", "%Y-%m-%d %H:%M:%S");
		entityBuilder.addTextBody("index_frequency", "10S");
		
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(dataSetData),
			ContentType.DEFAULT_BINARY,
			"temporaryfilename"
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.apiUrl + "/dataset");
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	public boolean updateDataSet(byte[] dataSetData, String feature, String dataSetId) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(dataSetData),
			ContentType.DEFAULT_BINARY,
			"temporaryfilename"
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPatch(this.apiUrl + "/dataset/" + dataSetId);
		request.setHeader("x-api-key", apiKey);
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	public String createModel(ModelType modelType, String dataSetId, String feature) throws ClientProtocolException, IOException {
		if (modelType == ModelType.ARIMA) {
			return createArimaModel(dataSetId, feature);
		}
		
		return "";
	}
	
	private String createArimaModel(String dataSetId, String feature) throws ClientProtocolException, IOException {
		CreateModelBody createModelBody = new CreateModelBody();
		createModelBody.type = ModelType.ARIMA;
		createModelBody.index = "date";
		createModelBody.indexSchema = "%Y-%m-%d %H:%M:%S";
		createModelBody.targetFeature = feature;
		createModelBody.hyperParameters.minElements = "50";
				
		Gson gson = new Gson();
		LOG.info(gson.toJson(createModelBody).toString());
		
		StringEntity requestBody = new StringEntity(
			gson.toJson(createModelBody).toString(),
			ContentType.APPLICATION_JSON
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.apiUrl + "/anomalies/" + dataSetId + "/model");
		request.setHeader("x-api-key", apiKey);
		request.setEntity(requestBody);
				
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		ModelDTO createModelResponse = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			ModelDTO.class
		);
		
		LOG.info(createModelResponse.id);
		
		return createModelResponse.id;
		
//		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	public boolean trainModel(String modelId) throws ClientProtocolException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.apiUrl + "/anomalies/model/" + modelId + "/train");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}

	public ModelDTO getModelDetails(String modelId) throws JsonSyntaxException, ParseException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpGet request = null;
		
		request = new HttpGet(this.apiUrl + "/anomalies/model/" + modelId + "/details");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		Gson gson = new Gson();
		
		ModelDTO getModelDetailsResponse = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			ModelDTO.class
		);
		
		return getModelDetailsResponse;
	}
	
	public AnomaliesReportDTO getAnomaliesReportForModel(String modelId) throws ClientProtocolException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpGet request = null;
		
		request = new HttpGet(this.apiUrl + "/model/" + modelId + "/anomaly");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(AnomalyDTO.class, new AnomalyDeserializer());
		
		Gson gson = gsonBuilder.create();
		
		LOG.info("Parsing response body");
		
		AnomaliesReportDTO[] anomaliesReport = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			AnomaliesReportDTO[].class
		);
		
		LOG.info("Sending response");
		
		return anomaliesReport[0];
	}
	
	private class AnomalyDeserializer implements JsonDeserializer<AnomalyDTO> {

		@Override
		public AnomalyDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject anomalyJson = json.getAsJsonObject();
			
			AnomalyDTO anomaly = new AnomalyDTO();
			
			anomaly.setDate(anomalyJson.get("date").getAsString());
			anomalyJson.remove("date");
			
			anomaly.distance = anomalyJson.get("distance").getAsDouble();
			anomalyJson.remove("distance");
			
			Set<Entry<String, JsonElement>> otherPropertiesSet = anomalyJson.entrySet();
		
			Entry<String, JsonElement> firstUnparsedEntry = otherPropertiesSet.iterator().next();
			
			LOG.fine(otherPropertiesSet.size() + " left to manage: " + firstUnparsedEntry);
			
			anomaly.feature = firstUnparsedEntry.getKey();
			anomaly.value = firstUnparsedEntry.getValue().getAsDouble();
			
			LOG.fine("Parsed anomaly: " + anomaly);
			
			return anomaly;
		}
		
	}
	
}
