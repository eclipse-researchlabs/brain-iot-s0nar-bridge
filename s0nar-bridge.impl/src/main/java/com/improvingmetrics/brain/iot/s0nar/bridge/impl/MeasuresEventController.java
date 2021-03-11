package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.util.Timer;

import org.eclipse.sensinact.brainiot.cwi.api.MeasuresEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.improvingmetrics.brain.iot.s0nar.service.S0narService;

public class MeasuresEventController implements EventManager {
	private static final Logger LOG = LoggerFactory.getLogger(MeasuresEventController.class);

	private S0narService s0narService;
	private MeasuresEvent measuresEvent;
	
	public MeasuresEventController(
		S0narService s0narService,
		Timer timer,
		MeasuresEvent measuresEvent
	) {
		this.s0narService = s0narService;
		this.measuresEvent = measuresEvent;
	}
	
	public void manageEvent() {
		
	}
}
