package com.improvingmetrics.brain.iot.s0nar.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.osgi.dto.DTO;

public class AnomalyDTO extends DTO {
	private String feature;
	private double value;
	private String date;
	private long timestamp;
	private double distance;
	
	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public void setDate(String date) {
		this.date = date;
		
		DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
		
		this.timestamp = Instant.from(formatter.parse(date)).toEpochMilli();
				
	}
	
	public String getDate() {
		return this.date;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
}
