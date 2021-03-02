package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.eclipse.sensinact.brainiot.cwi.api.AnomaliesDetectionMessage;
import org.eclipse.sensinact.brainiot.cwi.api.AnomalyDetectionMessage;
import org.eclipse.sensinact.brainiot.cwi.api.AnomalyStatus;
import org.eclipse.sensinact.brainiot.cwi.api.AnomalyType;
import org.eclipse.sensinact.brainiot.cwi.api.Measure;
import org.eclipse.sensinact.brainiot.cwi.api.MeasuresEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.google.gson.JsonSyntaxException;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;

@Component
@SmartBehaviourDefinition(
		consumed = {MeasuresEvent.class},
		filter = "(timestamp=*)",
        author = "Improving Metrics",
        name = "s0nar-bridge Behaviour",
        description = "Serves as a bridge between event bus communicatiosn and sOnar server"
)
@Designate(ocd = ComponentImpl.Config.class)
public class ComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	private static final Logger LOG = Logger.getLogger(ComponentImpl.class.getName());
	private final Map<String, String> SERVER_TO_DS_MAP = new HashMap<String, String>();
	
	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
	
	private Config config;
	
	@Reference
	private EventBus eventBus;
	
	private S0narService s0narService;
	
	private long lastAnomalyNotificationTS = 0l;
	
	@ObjectClassDefinition(
            name = "s0nar bridge",
            description = "Configuration for the s0nar bridge."
    )
    @interface Config {
        @AttributeDefinition(
    		type = AttributeType.STRING,
            name = "s0nar API URL",
            description = "The base URL for the s0nar API"
        )
        String s0narApiUrl() default "http://localhost:5004/s0nar/v1";

        @AttributeDefinition(
    		type = AttributeType.STRING,
            name = "s0nar API key",
            description = "The key to authenticate s0nar API calls"
        )
        String s0narApiKey() default "b5ea8e13f10d46c9ae33b392bb6d74de9c24e3a91fe44bb";
        
        @AttributeDefinition(
    		type = AttributeType.INTEGER,
            name = "Initial polling rate (s)",
            description = "The number of seconds to request a model state update from the server"
        )
        int initialUpdateRateSecs() default 10;
        
        @AttributeDefinition(
    		type = AttributeType.INTEGER,
            name = "Max polling rate(s)",
            description = "The max number of seconds to request a model state update from the server"
        )
        int maxUpdateRateSecs() default 180;
        
        @AttributeDefinition(
    		type = AttributeType.STRING,
            name = "Preloaded Data Set mapping",
            description = "Configures the preloaded datasets if any. Format: deviceId:dataSetId[,...]"
        )
        String preloadedDataSetMapping();// default "cft002:8fcf28f6-01d5-486c-8848-6ea4e5a22912";
    }
	
	public ComponentImpl() {}
	
	private void digestDataSetMapping(String mapping) {
		if (mapping != null && !mapping.isEmpty()) {
			String[] relations = mapping.split(",");
			
			for (String relation : relations) {
				String[] relationComponents = relation.split(":");
				
				this.SERVER_TO_DS_MAP.put(relationComponents[0], relationComponents[1]);
			}
		}
	}
	
	@Activate
	void activate(Config config) {
		LOG.info("S0nar bridge activated");
	    this.config = config;
	    
	    this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
	    
	    this.digestDataSetMapping(this.config.preloadedDataSetMapping());
    }

    @Modified
    void modify(Config config) {
        this.config = config;
        
        this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
        
        this.digestDataSetMapping(this.config.preloadedDataSetMapping());
    }

	@Deactivate
	void stop() {
		worker.shutdown();
	}
	
	@Override
	public void notify(BrainIoTEvent event) {
		LOG.info("Event Received: " + event.toString());

		if (event instanceof MeasuresEvent) {
			LOG.info("Measures event received: "+ event);
			this.manageMeasuresEvent((MeasuresEvent)event);
		}
	}
	
	private String getDataSetIdForDevice(String readerId) {
		return this.SERVER_TO_DS_MAP.get(readerId);
	}
	
	private String convertMilisToSchema(long milis) {
		OffsetDateTime dateTime = OffsetDateTime.ofInstant(
			Instant.ofEpochMilli(milis),
			ZoneOffset.UTC
		);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
		
		LOG.info("date:" + dateTime.format(formatter));
		
		return dateTime.format(formatter);
	}
	
	private void notifyWhenModelTrained(
		String feature,
		String modelId,
		int updateRateSec,
		int maxUpdateRateSec
	)throws JsonSyntaxException, ParseException, IOException {
		this.notifyWhenModelTrained(feature, modelId, updateRateSec, maxUpdateRateSec, 1);
	}
	
	private void notifyWhenModelTrained(
		String feature,
		String modelId,
		int updateRateSec,
		int maxUpdateRateSec,
		int updateCount
	) throws JsonSyntaxException, ParseException, IOException {
		ModelDTO modelDetails = this.s0narService.getModelDetails(modelId);
		
		LOG.info("Training status: " + modelDetails.getStatus());
		
		if (modelDetails.getStatus() == ModelStatus.TRAINING) {
			worker.schedule(() -> {
				try {
					notifyWhenModelTrained(feature, modelId, Math.min(updateRateSec * updateCount, maxUpdateRateSec), maxUpdateRateSec, updateCount + 1);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, e.getMessage(), e);
					throw new RuntimeException();
				}
			}, updateRateSec, TimeUnit.SECONDS);
		} else if (modelDetails.getStatus() == ModelStatus.FINISHED) {
			LOG.info("Model " + modelId + " training has finished");
			
			AnomaliesReportDTO anomaliesReport = this.s0narService.getAnomaliesReportForModel(modelId);
			
//			this.showAnomalies(anomaliesReport);
			
			this.notifyMeasureAnomalies(feature, anomaliesReport);
		}
	}
	
	private void showAnomalies(AnomaliesReportDTO anomaliesReport) throws ClientProtocolException, IOException {
		LOG.info("Detected anomalies:");
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			LOG.info(anomaly.toString());
		}
	}
	
	private void notifyMeasureAnomalies(String feature, AnomaliesReportDTO anomaliesReport) {
		boolean newAnomaliesFound = false;
		
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			if (anomaly.getTimestamp() > this.lastAnomalyNotificationTS) {
				AnomaliesDetectionMessage anomaliesMessage = new AnomaliesDetectionMessage();
				anomaliesMessage.anomalies = new HashMap<String, AnomalyDetectionMessage>();
		
				AnomalyDetectionMessage anomalyMessage = new AnomalyDetectionMessage();
				anomalyMessage.timestamp = Long.toString(anomaly.getTimestamp());
				anomalyMessage.type = AnomalyType.SPOT.name();
				anomalyMessage.status = AnomalyStatus.ANOMALY.name();
				
				anomaliesMessage.anomalies.put(feature, anomalyMessage);
				
				this.lastAnomalyNotificationTS = anomaly.getTimestamp();
		
				if (!anomaliesMessage.anomalies.isEmpty()) {
					newAnomaliesFound = true;
					
					LOG.info("Notifying anomalies: " + anomaliesMessage.anomalies);
					this.eventBus.deliver(anomaliesMessage);
				}
			}
		}
		
		if (!newAnomaliesFound) {
			LOG.info("No new anomalies detected");
		}
	}
	
	private void manageMeasuresEvent(MeasuresEvent event) {
		LOG.info("Digesting MeasuresEvent " + event);
		
		try {
			for (Measure measure : event.measures) {
				uploadMeasure(measure);
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private void uploadMeasure(Measure measure) throws ClientProtocolException, IOException {
		LOG.info("Uploading Measure " + measure);
		String dataSetId = this.getDataSetIdForDevice(measure.deviceId);
		
		if (dataSetId == null) {
			DataSetDTO dataSetDTO = new DataSetDTO();
			dataSetDTO.setName(measure.deviceId + "_" + measure.timestamp);
			
			DataSetDescriptorsDTO dataSetDescriptorsDTO = new DataSetDescriptorsDTO();
			dataSetDescriptorsDTO.setIndex("timestamp");
			dataSetDescriptorsDTO.setIndexFrequency("10S");
//			dataSetDescriptorsDTO.setIndexSchema("");
			dataSetDescriptorsDTO.setTargetFeature(measure.deviceId);
			dataSetDescriptorsDTO.setTargetFrequency("10s");

			dataSetDTO.setDescriptors(dataSetDescriptorsDTO);
			
			dataSetId = this.s0narService.createDataSet(
				dataSetDTO,
				this.parseMeasure(measure, true)
			);
			
			SERVER_TO_DS_MAP.put(measure.deviceId, dataSetId);
		} else {
			dataSetId = this.s0narService.updateDataSet(
				dataSetId,
				this.parseMeasure(measure, false)
			);
		}
		
		if (dataSetId != null) {
			DataSetDTO dataSet = this.s0narService.getDataSet(dataSetId);
			
			String modelId = this.s0narService.createModel(ModelType.ARIMA, dataSet);
			
			if (this.s0narService.trainModel(modelId)) {
				this.notifyWhenModelTrained(measure.deviceId, modelId, this.config.initialUpdateRateSecs(), this.config.maxUpdateRateSecs());
			}
		}
	}
	
	private byte[] parseMeasure(Measure measure, boolean appendHeader) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if (appendHeader) {
			stringBuilder.append("timestamp,");
			stringBuilder.append(measure.deviceId);
			stringBuilder.append(System.lineSeparator());
		}
		
		stringBuilder.append(measure.timestamp);
		stringBuilder.append(",");
		stringBuilder.append(measure.value);
		
		stringBuilder.append(System.lineSeparator());
		
		return stringBuilder.toString().getBytes();
	}
}
