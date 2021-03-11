package com.improvingmetrics.brain.iot.s0nar.emitter.impl;

import javax.xml.bind.annotation.XmlRootElement;

import org.osgi.dto.DTO;

@XmlRootElement
public class VoltageDTO extends DTO {
	public String index;
	public String deviceId;
	public double value;
}
