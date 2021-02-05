package com.improvingmetrics.brain.iot.demo.behaviour.demo.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

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
        name = "Example Behaviour",
        description = "Implements a demo behaviour."
)
@Designate(ocd = ComponentImpl.Config.class)
public class ComponentImpl implements SmartBehaviour<BrainIoTEvent> {
	
	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
	
	@ObjectClassDefinition(
            name = "Smart Behaviour",
            description = "Configuration for the Smart Behaviour."
    )
	@interface Config {
        enum LightColour {
            WHITE, RED, YELLOW, MAGENTA, GREEN, BLUE, CYAN
        }

        @AttributeDefinition(
        		type = AttributeType.INTEGER,
                name = "Time on",
                description = "How long light stays on after sensor is triggered (seconds)",
                min = "10",
                max = "300"
        )
        int duration() default 10;

        @AttributeDefinition(
                name = "Colour",
                description = "Colour light emits when activated"
        )
        LightColour colour() default LightColour.WHITE;
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
				request = new HttpGet("http://localhost:8888/PRUEBA_SICA");
			}
			
			client.execute(request);
		} catch (Exception e) {

		}
		
//		int oldValue = brightness.getAndSet(MAX_BRIGHTNESS);
//
//		if(oldValue == 0) {
//			worker.execute(this::updateBulb);
//		}
	}

	private void updateBulb() {
//		int value = brightness.getAndAccumulate(-1, (a,b) -> Math.max(0, a + b));
//
//		LightCommand command = new LightCommand();
//		command.brightness = value;
//		command.status = value > 0;
//
//		eventBus.deliver(command);
//
//		if (value != 0) {
//		    long delayMs = config.duration() * 1000 / MAX_BRIGHTNESS;
//			worker.schedule(this::updateBulb, delayMs, TimeUnit.MILLISECONDS);
//		}
	}
	
}
