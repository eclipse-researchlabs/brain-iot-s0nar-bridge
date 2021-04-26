package com.improvingmetrics.brain.iot.s0nar.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

public class S0narService {
	private static final Logger LOG = LoggerFactory.getLogger(S0narService.class);
			
	private String apiUrl;
	private String apiKey;
	
	public S0narService(String apiUrl, String apiKey) {
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
	}
	
	public String createDataSet(byte[] dataSetData, String feature) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addTextBody("name", "thename");
		entityBuilder.addTextBody("target_index", "date");
		entityBuilder.addTextBody("target_feature", feature);
		entityBuilder.addTextBody("name", feature);
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
		
		
		HttpEntityEnclosingRequestBase request = new HttpPost(this.apiUrl + "/dataset");
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			LOG.info(response.toString());
			
			Gson gson = new Gson();
			
			DataSetDTO dataSet = gson.fromJson(
				EntityUtils.toString(response.getEntity()),
				DataSetDTO.class
			);
			
			return dataSet.getId();
		} else {
			return null;
		}
	}
	
	public String createDataSet(
		DataSetDTO dataSetDTO,
		byte[] dataSetData
	) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addTextBody("name", dataSetDTO.getName());
		entityBuilder.addTextBody("target_index", dataSetDTO.getDescriptors().getIndex());
		if (dataSetDTO.getDescriptors().getIndexSchema() != null) {
			entityBuilder.addTextBody("index_schema", dataSetDTO.getDescriptors().getIndexSchema());
		}
		entityBuilder.addTextBody("index_frequency", dataSetDTO.getDescriptors().getIndexFrequency());
		entityBuilder.addTextBody("target_feature", dataSetDTO.getDescriptors().getTargetFeature());
		entityBuilder.addTextBody("target_frequency", dataSetDTO.getDescriptors().getTargetFrequency());
		
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(dataSetData),
			ContentType.DEFAULT_BINARY,
			dataSetDTO.getName()
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = new HttpPost(this.apiUrl + "/dataset");
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			LOG.info(response.toString());
			
			Gson gson = new Gson();
			
			DataSetDTO dataSet = gson.fromJson(
				EntityUtils.toString(response.getEntity()),
				DataSetDTO.class
			);
			
			return dataSet.getId();
		} else {
			return null;
		}
	}
	
	public String updateDataSet(String dataSetId, byte[] dataSetData) throws ClientProtocolException, IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entityBuilder.addBinaryBody(
			"dataset",
			new ByteArrayInputStream(dataSetData),
			ContentType.DEFAULT_BINARY,
			"temporaryfilename"
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = new HttpPatch(this.apiUrl + "/dataset/" + dataSetId);
		request.setHeader("x-api-key", apiKey);
		request.setEntity(entityBuilder.build());
		
		HttpResponse response = client.execute(request);
		
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			LOG.info(response.toString());
			
			Gson gson = new Gson();
			
			DataSetDTO dataSet = gson.fromJson(
				EntityUtils.toString(response.getEntity()),
				DataSetDTO.class
			);
			
			return dataSet.getId();
		} else {
			return null;
		}
	}
	
	public DataSetDTO getDataSet(String dataSetId) throws JsonSyntaxException, ParseException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpGet request = new HttpGet(this.apiUrl + "/dataset/" + dataSetId + "/details");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.info(response.toString());
		
		Gson gson = new Gson();
		
		DataSetDTO dataSetDetails = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			DataSetDTO.class
		);
		
		return dataSetDetails;
	}
	
	public String createModel(ModelType modelType, DataSetDTO dataset, double anomalyDetectionThreshold) throws ClientProtocolException, IOException {
		if (modelType == ModelType.ARIMA) {
			return this.createArimaModel(dataset, anomalyDetectionThreshold);
		}
		
		return null;
	}
	
	private String createArimaModel(DataSetDTO dataset, double anomalyDetectionThreshold) throws ClientProtocolException, IOException {
		CreateModelBodyDTO createModelBody = new CreateModelBodyDTO();
		createModelBody.type = ModelType.ARIMA;
		createModelBody.index = dataset.getDescriptors().getIndex();
		createModelBody.indexSchema = dataset.getDescriptors().getIndexSchema();
		createModelBody.targetFeature = dataset.getDescriptors().getTargetFeature();
		createModelBody.hyperParameters.minElements = "50";
		createModelBody.hyperParameters.threshold = anomalyDetectionThreshold;
				
		Gson gson = new Gson();
		LOG.debug(gson.toJson(createModelBody).toString());
		
		StringEntity requestBody = new StringEntity(
			gson.toJson(createModelBody).toString(),
			ContentType.APPLICATION_JSON
		);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.apiUrl + "/anomalies/" + dataset.getId() + "/model");
		request.setHeader("x-api-key", apiKey);
		request.setEntity(requestBody);
				
		HttpResponse response = client.execute(request);
		
		LOG.debug(response.toString());
		
		ModelDTO createModelResponse = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			ModelDTO.class
		);
		
		return createModelResponse.getId();
	}
	
	public boolean trainModel(String modelId) throws ClientProtocolException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpEntityEnclosingRequestBase request = null;
		
		request = new HttpPost(this.apiUrl + "/anomalies/model/" + modelId + "/train");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.debug(response.toString());
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}

	public ModelDTO getModelDetails(String modelId) throws JsonSyntaxException, ParseException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpGet request = new HttpGet(this.apiUrl + "/anomalies/model/" + modelId + "/details");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		
		LOG.debug(response.toString());
		
		Gson gson = new Gson();
		
		ModelDTO getModelDetailsResponse = gson.fromJson(
			EntityUtils.toString(response.getEntity()),
			ModelDTO.class
		);
		
		return getModelDetailsResponse;
	}
	
	public AnomaliesReportDTO getAnomaliesReportForModel(String modelId) throws ClientProtocolException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		
		HttpGet request = new HttpGet(this.apiUrl + "/model/" + modelId + "/anomaly");
		request.setHeader("x-api-key", apiKey);
		
		HttpResponse response = client.execute(request);
		String responseBody = EntityUtils.toString(response.getEntity());
		
		LOG.debug(response.toString());
		LOG.debug(responseBody);
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(AnomalyDTO.class, new AnomalyDeserializer());
		
		try {
		
			Gson gson = gsonBuilder.create();
			
			AnomaliesReportDTO[] anomalyReports = gson.fromJson(
				responseBody,
				AnomaliesReportDTO[].class
			);
			
			LOG.info("Anomaly reports got: " + Arrays.toString(anomalyReports));
		
			return anomalyReports[0];
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw e;
		}
	}
	
	private class AnomalyDeserializer implements JsonDeserializer<AnomalyDTO> {

		@Override
		public AnomalyDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject anomalyJson = json.getAsJsonObject();
			
			LOG.trace("Parsing JSON anomaly " + anomalyJson);
			
			AnomalyDTO anomaly = new AnomalyDTO();
			
			JsonElement jsonDate = anomalyJson.get("date");
			if (jsonDate != null) {
				anomaly.setDate(jsonDate.getAsString());
				anomalyJson.remove("date");
			}
			
			JsonElement jsonTimestamp = anomalyJson.get("timestamp");
			if (jsonTimestamp != null) {
				anomaly.setTimestamp(Long.parseLong(jsonTimestamp.getAsString()));
				anomalyJson.remove("timestamp");
			}
			
			JsonElement jsonIndex = anomalyJson.get("index");
			if (jsonIndex != null) {
				anomaly.setTimestamp(convertSchemaToMilis(jsonIndex.getAsString()));
				anomalyJson.remove("index");
			}
			
			anomaly.setDistance(anomalyJson.get("distance").getAsDouble());
			anomalyJson.remove("distance");
			
			Set<Entry<String, JsonElement>> otherPropertiesSet = anomalyJson.entrySet();
		
			Entry<String, JsonElement> firstUnparsedEntry = otherPropertiesSet.iterator().next();
			
			LOG.trace(otherPropertiesSet.size() + " left to manage: " + firstUnparsedEntry);
			
			anomaly.setFeature(firstUnparsedEntry.getKey());
			anomaly.setValue(firstUnparsedEntry.getValue().getAsDouble());
			
			LOG.trace("Parsed anomaly: " + anomaly);
			
			return anomaly;
		}
		
	}
	
	private Long convertSchemaToMilis(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
			
		return LocalDateTime.parse(date, formatter).atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
	}
}
