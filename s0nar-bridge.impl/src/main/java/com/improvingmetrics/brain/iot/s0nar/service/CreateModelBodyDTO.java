package com.improvingmetrics.brain.iot.s0nar.service;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class CreateModelBodyDTO extends DTO {
	public class HyperParameters {
		@SerializedName(value = "min_elements")
		public String minElements;
		
		public double threshold;
	}
	
	public ModelType type;
	public String index;
	
	@SerializedName(value = "index_schema")
	public String indexSchema;
	
	@SerializedName(value = "target_feature")
	public String targetFeature;
	
	@SerializedName(value = "hyper_parameters")
	public HyperParameters hyperParameters = new HyperParameters();
}
