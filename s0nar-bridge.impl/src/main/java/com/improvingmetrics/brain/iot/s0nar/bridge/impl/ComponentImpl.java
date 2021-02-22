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
import java.util.logging.Logger;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.google.gson.JsonSyntaxException;
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
@Designate(ocd = ComponentImpl.Config.class)
public class ComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	private static final Logger LOG = Logger.getLogger(ComponentImpl.class.getName());
	private final Map<String, String> SERVER_TO_DS_MAP = new HashMap<String, String>();
	
	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
	
	private Config config;
	
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
    }
	
	public ComponentImpl() {
//		SERVER_TO_DS_MAP.put("cft002", "6797cddc-a9ed-4768-b4a0-8a49340a6eb2");
		
//		SERVER_TO_DS_MAP.put("cft002", "5f9eec49-f2a8-446c-b42d-6939354231e0");
		
		
	}
	
	@Activate
	void activate(Config config) {
		LOG.info("S0nar bridge activated");			
	    this.config = config;
	    
	    this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
    }

    @Modified
    void modify(Config config) {
        this.config = config;
        
        this.s0narService = new S0narService(
    		this.config.s0narApiUrl(),
    		this.config.s0narApiKey()
		);
    }

	@Deactivate
	void stop() {
		worker.shutdown();
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
			this.uploadSicaRead(sicaRead);
		} catch (Exception e) {
			LOG.severe(e.toString());
		}
	}
		
	private String getDataSetIdForSicaRead(String readerId) {
		return this.SERVER_TO_DS_MAP.get(readerId);
	}
	
	private void uploadSicaRead(SicaReadResponse sicaRead) throws ClientProtocolException, IOException {
		String feature = "cft002";
		
		String dataSetId = getDataSetIdForSicaRead(feature);
		
		if (dataSetId == null) {
			dataSetId = this.s0narService.createDataSet(
				this.parseEvent(sicaRead, true),
				feature
			);
			
			SERVER_TO_DS_MAP.put(feature, dataSetId);
		} else {
			dataSetId = this.s0narService.updateDataSet(
				this.parseEvent(sicaRead, false),
				feature,
				dataSetId
			);
		}
		
		if (dataSetId != null) {
			String modelId = this.s0narService.createModel(ModelType.ARIMA, dataSetId, feature);
			
			if (this.s0narService.trainModel(modelId)) {
				this.notifyWhenModelTrained(modelId);
			}
		}
	}
	
	private byte[] parseEvent(SicaReadResponse sicaRead, boolean appendHeader) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if (appendHeader) {
			stringBuilder.append("date,cft002,cft003,dft001,eft001");
			stringBuilder.append(System.lineSeparator());
		}
		
		stringBuilder.append(convertMilisToSchema(sicaRead.timestamp));
		
		for (double entry : sicaRead.value) {
			stringBuilder.append(",");
			stringBuilder.append(entry);
		}
		
		stringBuilder.append(System.lineSeparator());
		
		return stringBuilder.toString().getBytes();
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
	
	private void notifyWhenModelTrained(String modelId) throws JsonSyntaxException, ParseException, IOException {
		ModelDTO modelDetails = this.s0narService.getModelDetails(modelId);
		
		LOG.info("Training status: " + modelDetails.getStatus());
		
		if (modelDetails.getStatus() == ModelStatus.TRAINING) {
			worker.schedule(() -> {
				try {
					notifyWhenModelTrained(modelId);
				} catch (Exception e) {
					throw new RuntimeException();
				}
			}, 10, TimeUnit.SECONDS);
		} else {
			LOG.info("Model " + modelId + " training has finished");
			
			AnomaliesReportDTO anomaliesReport = this.s0narService.getAnomaliesReportForModel(modelId);
			
			this.showAnomalies(anomaliesReport);
			
			this.notifySicaAnomalies(anomaliesReport);
		}
	}
	
	private void showAnomalies(AnomaliesReportDTO anomaliesReport) throws ClientProtocolException, IOException {
		LOG.info("Detected anomalies:");
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			LOG.info(anomaly.toString());
		}
	}
	
	private void notifySicaAnomalies(AnomaliesReportDTO anomaliesReport) {
		for (AnomalyDTO anomaly : anomaliesReport.getAnomalies()) {
			if (anomaly.getTimestamp() > this.lastAnomalyNotificationTS) {
				this.notifySicaAnomaly(anomaly);
				
				this.lastAnomalyNotificationTS = anomaly.getTimestamp();
			}
		}
	}
	
	private void notifySicaAnomaly(AnomalyDTO anomaly) {
		LOG.info("Notifying anomaly: " + anomaly);
		
		// TODO: Notify through event bus
	}
}
