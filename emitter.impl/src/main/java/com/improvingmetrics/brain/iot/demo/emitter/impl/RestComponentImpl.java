package com.improvingmetrics.brain.iot.demo.emitter.impl;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import com.improvingmetrics.brain.iot.demo.emitter.api.EmitterMessageDTO;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.EventBus;
import fr.cea.brain.iot.sensinact.api.sica.SicaReadResponse;

@Component(service=RestComponentImpl.class)
@JaxrsResource
//@HttpWhiteboardResource(pattern="/emitter-ui/*", prefix="/static")
// SmartBehaviourDefinition is just so example sensor is added to repository
@SmartBehaviourDefinition(consumed = {}, // this component does not consume events
        author = "Improving Metrics", name = "Example Emitter",
        description = "Implements an emitter.")
public class RestComponentImpl {

	@Reference
	private EventBus eventBus;
	
    @Path("emitter")
    @POST
    public void triggerEmitter() {
        eventBus.deliver(new EmitterMessageDTO());
    }
    
    @Path("sica")
    @POST
    public void triggerSica() {
    	SicaReadResponse sicaRead = new SicaReadResponse();
//    	sicaRead.timestamp = System.currentTimeMillis();
    	sicaRead.timestamp = 1609339271000l;
    	sicaRead.value = new double[]{2.282772,1.612903,0.5173611,0.4566743};
    	
        eventBus.deliver(sicaRead);
    }
    
}
