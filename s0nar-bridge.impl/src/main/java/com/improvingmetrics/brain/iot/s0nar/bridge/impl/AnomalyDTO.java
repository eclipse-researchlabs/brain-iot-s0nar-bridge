package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;

public class AnomalyDTO {
	public String feature;
	public double value;
	private String date;
	public long timestamp;
	public double distance;
	
	public void setDate(String date) {
		this.date = date;
		
		DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
		
		this.timestamp = Instant.from(formatter.parse(date)).toEpochMilli();
				
	}
	
	public String getDate() {
		return this.date;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		
		return strBuilder
			.append("<")
			.append("feature: ")
			.append(this.feature)
			.append(", value: ")
			.append(this.value)
			.append(", date: ")
			.append(this.date)
			.append(", timestamp: ")
			.append(this.timestamp)
			.append(", distance: ")
			.append(this.distance)
			.append(">")
			.toString();
	}
}
