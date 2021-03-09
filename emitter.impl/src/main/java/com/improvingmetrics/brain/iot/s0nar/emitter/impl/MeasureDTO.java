package com.improvingmetrics.brain.iot.s0nar.emitter.impl;

import javax.xml.bind.annotation.XmlRootElement;

import org.osgi.dto.DTO;

@XmlRootElement
public class MeasureDTO extends DTO {
	public long timestamp;
	public String deviceId;
	public double value;
}
