package com.improvingmetrics.brain.iot.demo.receiver.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import com.improvingmetrics.brain.iot.demo.receiver.api.ReceiverCommand;
import com.improvingmetrics.brain.iot.demo.receiver.api.ReceiverQuery;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;

@Component(service= {SmartBehaviour.class, RestComponentImpl.class})
@JaxrsResource
@HttpWhiteboardResource(pattern="/demo-ui/*", prefix="/static")
@JSONRequired
@SmartBehaviourDefinition(
	consumed = {ReceiverCommand.class, ReceiverQuery.class},
	filter = "(brightness=*)",
    author = "Improving Metrics",
    name = "Receiver demo",
    description = "Tries to show off how events work"
)
public class RestComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	
	@Reference
	private EventBus eventBus;
	
	@Override
	public void notify(BrainIoTEvent event) {
		if (event instanceof ReceiverQuery) {
			System.out.println("ReceiverQuery event: " + event.getClass());
		} else {
			System.out.println("Argh! Received an unknown event type " + event.getClass());
		}
	}
	
}
