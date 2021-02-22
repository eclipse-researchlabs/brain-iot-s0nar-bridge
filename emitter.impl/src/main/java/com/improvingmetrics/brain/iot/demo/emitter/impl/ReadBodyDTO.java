package com.improvingmetrics.brain.iot.demo.emitter.impl;

import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReadBodyDTO {
	public String timestamp;
	public double[] reads;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append("<timestamp: ")
			.append(this.timestamp)
			.append(", reads:");
		
		for (double read : this.reads) {
			sb.append(read).append(",");
		}
		
		sb.append(">");
		
		return sb.toString();		
	}
}
