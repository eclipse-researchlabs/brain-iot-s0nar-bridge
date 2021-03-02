package com.improvingmetrics.brain.iot.demo.emitter.impl;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.sensinact.brainiot.cwi.api.Measure;
import org.eclipse.sensinact.brainiot.cwi.api.MeasuresEvent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.EventBus;

@Component(service=RestComponentImpl.class)
@JaxrsResource
// SmartBehaviourDefinition is just so example sensor is added to repository
@SmartBehaviourDefinition(consumed = {}, // this component does not consume events
        author = "Improving Metrics", name = "Example Emitter",
        description = "Implements an emitter.")
public class RestComponentImpl {
	private static final Logger LOG = Logger.getLogger(RestComponentImpl.class.getName());

	@Reference
	private EventBus eventBus;
	
    @Path("measure-demo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces()
    public Response triggerMeasurement(MeasureDTO[] measures) {
    	MeasuresEvent measuresEvent = new MeasuresEvent();
    	measuresEvent.measures = new ArrayList<>();
    	
    	for (MeasureDTO measure : measures) {
    		LOG.fine("Adding measure" + measure.toString());
    		Measure measureForEvent = new Measure();
    		
//    		measureForEvent.timestamp = measure.timestamp;
    		measureForEvent.timestamp = System.currentTimeMillis();
    		measureForEvent.deviceId = measure.deviceId;
    		measureForEvent.value = measure.value;
    		
    		measuresEvent.measures.add(measureForEvent);
    	}
    	
    	if (!measuresEvent.measures.isEmpty()) {
    		this.eventBus.deliver(measuresEvent);
    		LOG.info("Delivered message: " + measuresEvent);
    	}
    	
    	return Response.status(Response.Status.OK).build();
    }
    
}
