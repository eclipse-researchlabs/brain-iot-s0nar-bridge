package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AnomaliesReportDTO {
	@SerializedName(value = "_id")
	public String id;
	
	@SerializedName(value = "creation_date")
	public String creationDate;
	
	@SerializedName(value = "model")
	public String modelId;
	
//	public ParametersDTO params;
	
	public List<AnomalyDTO> anomalies;
	
//	public class ParametersDTO {
//		public double threshold;
//	}
}
