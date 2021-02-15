package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import com.google.gson.annotations.SerializedName;

public class CreateModelBody {
	public class HyperParameters {
		@SerializedName(value = "min_elements")
		public String minElements; 
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
