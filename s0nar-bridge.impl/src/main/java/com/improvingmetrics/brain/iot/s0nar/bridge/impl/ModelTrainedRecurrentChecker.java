package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.improvingmetrics.brain.iot.s0nar.service.ModelDTO;
import com.improvingmetrics.brain.iot.s0nar.service.ModelStatus;
import com.improvingmetrics.brain.iot.s0nar.service.S0narService;

public class ModelTrainedRecurrentChecker extends TimerTask {
	private String modelId;
	private S0narService s0narService;
	private Runnable modelTrainedCallback;
	
	private static final Logger LOG = LoggerFactory.getLogger(ModelTrainedRecurrentChecker.class);

	public ModelTrainedRecurrentChecker(
		String modelId,
		S0narService s0narService,
		Runnable modelTrainedCallback
	) {
		this.modelId = modelId;
		this.s0narService = s0narService;
		this.modelTrainedCallback = modelTrainedCallback;
	}

	@Override
	public void run() {
		try {
			ModelDTO modelDetails = this.s0narService.getModelDetails(this.modelId);
			LOG.debug("Training status: " + modelDetails.getStatus());
			
			if (modelDetails.getStatus() == ModelStatus.FINISHED) {
				LOG.info("Model " + this.modelId + " training has finished");
				this.modelTrainedCallback.run();
				this.cancel();
			} else if (modelDetails.getStatus() == ModelStatus.FAILED) {
				LOG.info("Model " + this.modelId + " training has failed (" + modelDetails.getRaisedException() + ")");
				this.cancel();
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
