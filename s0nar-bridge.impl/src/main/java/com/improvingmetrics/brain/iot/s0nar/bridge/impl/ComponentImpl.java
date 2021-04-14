package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Timer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.improvingmetrics.brain.iot.s0nar.service.AnomaliesReportDTO;
import com.improvingmetrics.brain.iot.s0nar.service.AnomalyDTO;
import com.improvingmetrics.brain.iot.s0nar.service.DataSetDTO;
import com.improvingmetrics.brain.iot.s0nar.service.DataSetDescriptorsDTO;
import com.improvingmetrics.brain.iot.s0nar.service.ModelType;
import com.improvingmetrics.brain.iot.s0nar.service.S0narService;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.robot.events.BatteryVoltage;

@Component
@SmartBehaviourDefinition(
		consumed = {MeasuresEvent.class, BatteryVoltage.class},
		filter = "(timestamp=*)",
        author = "Improving Metrics",
        name = "s0nar-bridge Behaviour",
        description = "Serves as a bridge between event bus communicatiosn and sOnar server"
)
@Designate(ocd = ComponentImpl.Config.class)
public class ComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(ComponentImpl.class);
	
	private final DeviceStatusManager deviceStatusManager = new DeviceStatusManager();
	
	private final Timer timer = new Timer();
	
	private Config config;
	
	@Reference
	private EventBus eventBus;
	
	private S0narService s0narService;
	
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
            name = "Initial polling delay (s)",
            description = "The number of seconds to wait before requesting a model state update from the server"
        )
        int modelStatusUpdateDelay() default 10;
        
        @AttributeDefinition(
    		type = AttributeType.INTEGER,
            name = "Polling period (s)",
            description = "The number of seconds to request a model state update from the server while training"
        )
        int modelStatusUpdatePeriod() default 60;
        
        @AttributeDefinition(
    		type = AttributeType.INTEGER,
            name = "Minimum event period (s)",
            description = "The minimum number of seconds between the management of events"
        )
        int minEventPeriodSecs() default 0;
        
        @AttributeDefinition(
    		type = AttributeType.STRING,
            name = "Preloaded Data Set mapping",
            description = "Configures the preloaded datasets if any. Format: deviceId:dataSetId[,...]"
        )
        String preloadedDataSetMapping();// default "cft002:8fcf28f6-01d5-486c-8848-6ea4e5a22912";
    }
	
	public ComponentImpl() {}
	
	@Activate
	void activate(Config config) {
		LOG.info("S0nar bridge activated");
	    this.config = config;
	    
	    this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
	    
	    this.deviceStatusManager.configure(this.config.preloadedDataSetMapping());
    }

    @Modified
    void modify(Config config) {
        this.config = config;
        
        this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
        
        this.deviceStatusManager.configure(this.config.preloadedDataSetMapping());
    }

	@Deactivate
	void stop() {
		timer.cancel();
	}
	
	@Override
	public void notify(BrainIoTEvent event) {
		LOG.info("Event Received: " + event.toString());

		if (event instanceof MeasuresEvent) {
			LOG.info("Measures event received: "+ event);
//			MeasuresEventController eventController = new MeasuresEventController(
//				this.s0narService,
//				this.timer,
//				(MeasuresEvent)event
//			);
//			
//			eventController.manageEvent();
			
			this.manageMeasuresEvent((MeasuresEvent)event);
		} else if (event instanceof BatteryVoltage) {
			LOG.info("Battery event received: "+ event);
			this.manageBatteryEvent((BatteryVoltage)event);
		}
	}
	
	private String convertMilisToSchema(long milis) {
		OffsetDateTime dateTime = OffsetDateTime.ofInstant(
			Instant.ofEpochMilli(milis),
			ZoneOffset.UTC
		);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
		
		LOG.debug("date:" + dateTime.format(formatter));
		
		return dateTime.format(formatter);
	}
	
	private void waitForModelToBeTrained(
		String modelId,
		S0narService s0narService,
		Runnable callback
	) {
		timer.schedule(
			new ModelTrainedRecurrentChecker(modelId, s0narService, callback),
			this.config.modelStatusUpdateDelay() * 1000,
			this.config.modelStatusUpdatePeriod() * 1000
		);
	}
	
	private void showAnomalies(AnomaliesReportDTO anomaliesReport) throws ClientProtocolException, IOException {
		LOG.info("Detected anomalies:");
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			LOG.info(anomaly.toString());
		}
	}
	
	private void notifyMeasureAnomalies(String deviceId, AnomaliesReportDTO anomaliesReport) {
		boolean newAnomaliesFound = false;
		
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			if (anomaly.getTimestamp() > this.deviceStatusManager.getLastAnomalyTSForDevice(deviceId)) {
				AnomaliesDetectionMessage anomaliesMessage = new AnomaliesDetectionMessage();
				anomaliesMessage.anomalies = new HashMap<String, AnomalyDetectionMessage>();
		
				AnomalyDetectionMessage anomalyMessage = new AnomalyDetectionMessage();
				anomalyMessage.timestamp = Long.toString(anomaly.getTimestamp());
				anomalyMessage.type = AnomalyType.SPOT.name();
				anomalyMessage.status = AnomalyStatus.ANOMALY.name();
				
				anomaliesMessage.anomalies.put(deviceId, anomalyMessage);
				
				this.deviceStatusManager.setLastAnomalyTSForDevice(deviceId, anomaly.getTimestamp());
		
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
		LOG.debug("Digesting MeasuresEvent " + event);
		
		try {
			for (Measure measure : event.measures) {
				if (
					this.deviceStatusManager.getSecsFromLastManagedEventForDevice(measure.deviceId) >=
					this.config.minEventPeriodSecs()
				) {
					this.deviceStatusManager.resetLastManagedEventTSFromDevice(measure.deviceId);
					String dataSetId = this.uploadMeasure(measure);
					
					this.findAnomalies(measure.deviceId, dataSetId);
				} else {
					LOG.debug(
						"Minimum of " +
						this.config.minEventPeriodSecs() +
						" seconds have not yet elapsed. Ignoring event."
					);
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private String uploadMeasure(Measure measure) throws ClientProtocolException, IOException {
		LOG.debug("Uploading Measure " + measure);
		String dataSetId = this.deviceStatusManager.getDataSetIdForDevice(measure.deviceId);
		
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
			
			this.deviceStatusManager.setDataSetIdForDevice(measure.deviceId, dataSetId);
		} else {
			dataSetId = this.s0narService.updateDataSet(
				dataSetId,
				this.parseMeasure(measure, false)
			);
		}
		
		return dataSetId;
	}
	
	private void findMeasureAnomalies(String deviceId, String dataSetId) throws JsonSyntaxException, ParseException, IOException {
		if (dataSetId != null) {
			DataSetDTO dataSet = this.s0narService.getDataSet(dataSetId);
			
			String modelId = this.s0narService.createModel(ModelType.ARIMA, dataSet);
			
			if (this.s0narService.trainModel(modelId)) {
				this.waitForModelToBeTrained(modelId, this.s0narService, () -> {
					try {
						AnomaliesReportDTO anomaliesReport = this.s0narService.getAnomaliesReportForModel(modelId);
						
		//				this.showAnomalies(anomaliesReport);
						
						this.notifyMeasureAnomalies(deviceId, anomaliesReport);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				});
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
	
	private void manageBatteryEvent(BatteryVoltage batteryVoltage) {
		String deviceId = Integer.toString(batteryVoltage.robotID);
		
		if (
			this.deviceStatusManager.getSecsFromLastManagedEventForDevice(deviceId) >=
			this.config.minEventPeriodSecs()	
		) {
			this.deviceStatusManager.resetLastManagedEventTSFromDevice(deviceId);
			LOG.debug("Digesting BatteryVoltage event " + batteryVoltage);
			try {
				String dataSetId = this.uploadBatteryVoltage(batteryVoltage);

				this.findAnomalies(deviceId, dataSetId);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		} else {
			LOG.debug(
				"Minimum of " +
				this.config.minEventPeriodSecs() +
				" seconds have not yet elapsed. Ignoring event."
			);
		}
	}
	
	private String uploadBatteryVoltage(BatteryVoltage batteryVoltage) throws ClientProtocolException, IOException {
		LOG.debug("Uploading Battery Voltage measure " + batteryVoltage);
		String dataSetId = this.deviceStatusManager.getDataSetIdForDevice(Integer.toString(batteryVoltage.robotID));
		
		if (dataSetId == null) {
			DataSetDTO dataSetDTO = new DataSetDTO();
			dataSetDTO.setName(batteryVoltage.robotID + "_" + System.currentTimeMillis());
			
			DataSetDescriptorsDTO dataSetDescriptorsDTO = new DataSetDescriptorsDTO();
			dataSetDescriptorsDTO.setIndex("index");
			dataSetDescriptorsDTO.setIndexFrequency("10S");
			dataSetDescriptorsDTO.setIndexSchema("%Y-%m-%d %H:%M:%S");
			dataSetDescriptorsDTO.setTargetFeature(Integer.toString(batteryVoltage.robotID));
			dataSetDescriptorsDTO.setTargetFrequency("10s");

			dataSetDTO.setDescriptors(dataSetDescriptorsDTO);
			
			dataSetId = this.s0narService.createDataSet(
				dataSetDTO,
				this.parseBatteryVoltage(batteryVoltage, true)
			);
			
			this.deviceStatusManager.setDataSetIdForDevice(
				Integer.toString(batteryVoltage.robotID),
				dataSetId
			);
		} else {
			dataSetId = this.s0narService.updateDataSet(
				dataSetId,
				this.parseBatteryVoltage(batteryVoltage, false)
			);
		}
		
		return dataSetId;
	}
	
	private byte[] parseBatteryVoltage(BatteryVoltage batteryVoltage, boolean appendHeader) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if (appendHeader) {
			stringBuilder.append("index,");
			stringBuilder.append(batteryVoltage.robotID);
			stringBuilder.append(System.lineSeparator());
		}
		
		stringBuilder.append(batteryVoltage.index);
		stringBuilder.append(",");
		stringBuilder.append(batteryVoltage.target);
		
		stringBuilder.append(System.lineSeparator());
		
		return stringBuilder.toString().getBytes();
	}
	
	private void findBatteryVoltageAnomalies(String deviceId, String dataSetId) throws JsonSyntaxException, ParseException, IOException {
		if (dataSetId != null) {
			DataSetDTO dataSet = this.s0narService.getDataSet(dataSetId);
			
			String modelId = this.s0narService.createModel(ModelType.ARIMA, dataSet);
			
			if (this.s0narService.trainModel(modelId)) {
				this.waitForModelToBeTrained(modelId, this.s0narService, () -> {
					try {
						AnomaliesReportDTO anomaliesReport = this.s0narService.getAnomaliesReportForModel(modelId);
						
		//				this.showAnomalies(anomaliesReport);
						
						this.notifyBatteryVoltageAnomalies(deviceId, anomaliesReport);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				});
			}
		}
	}
	
	private void notifyBatteryVoltageAnomalies(String deviceId, AnomaliesReportDTO anomaliesReport) {
		boolean newAnomaliesFound = false;
		
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			if (anomaly.getTimestamp() > this.deviceStatusManager.getLastAnomalyTSForDevice(deviceId)) {
				AnomaliesDetectionMessage anomaliesMessage = new AnomaliesDetectionMessage();
				anomaliesMessage.anomalies = new HashMap<String, AnomalyDetectionMessage>();
		
				AnomalyDetectionMessage anomalyMessage = new AnomalyDetectionMessage();
				anomalyMessage.timestamp = Long.toString(anomaly.getTimestamp());
				anomalyMessage.type = AnomalyType.SPOT.name();
				anomalyMessage.status = AnomalyStatus.ANOMALY.name();
				
				anomaliesMessage.anomalies.put(deviceId, anomalyMessage);
				
				this.deviceStatusManager.setLastAnomalyTSForDevice(deviceId, anomaly.getTimestamp());
		
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
	
	private void findAnomalies(String deviceId, String dataSetId) throws JsonSyntaxException, ParseException, IOException {
		if (dataSetId != null) {
			DataSetDTO dataSet = this.s0narService.getDataSet(dataSetId);
			
			String modelId = this.s0narService.createModel(ModelType.ARIMA, dataSet);
			
			if (this.s0narService.trainModel(modelId)) {
				this.waitForModelToBeTrained(modelId, this.s0narService, () -> {
					try {
						AnomaliesReportDTO anomaliesReport = this.s0narService.getAnomaliesReportForModel(modelId);
						
		//				this.showAnomalies(anomaliesReport);
						
						this.notifyAnomalies(deviceId, anomaliesReport);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				});
			}
		}
	}
	
	private void notifyAnomalies(String deviceId, AnomaliesReportDTO anomaliesReport) {
		boolean newAnomaliesFound = false;
		
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			if (anomaly.getTimestamp() > this.deviceStatusManager.getLastAnomalyTSForDevice(deviceId)) {
				AnomaliesDetectionMessage anomaliesMessage = new AnomaliesDetectionMessage();
				anomaliesMessage.anomalies = new HashMap<String, AnomalyDetectionMessage>();
		
				AnomalyDetectionMessage anomalyMessage = new AnomalyDetectionMessage();
				anomalyMessage.timestamp = Long.toString(anomaly.getTimestamp());
				anomalyMessage.type = AnomalyType.SPOT.name();
				anomalyMessage.status = AnomalyStatus.ANOMALY.name();
				
				anomaliesMessage.anomalies.put(deviceId, anomalyMessage);
				
				this.deviceStatusManager.setLastAnomalyTSForDevice(deviceId, anomaly.getTimestamp());
		
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
}
