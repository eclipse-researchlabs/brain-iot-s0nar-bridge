package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class DataSetDescriptorsDTO extends DTO {
	private String index;
	
	@SerializedName("index_frequency")
	private String indexFrequency;
	
	@SerializedName("index_schema")
	private String indexSchema;
	
	@SerializedName("target_feature")
	private String targetFeature;
	
	@SerializedName("target_frequency")
	private String targetFrequency;

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getIndexFrequency() {
		return indexFrequency;
	}

	public void setIndexFrequency(String indexFrequency) {
		this.indexFrequency = indexFrequency;
	}

	public String getIndexSchema() {
		return indexSchema;
	}

	public void setIndexSchema(String indexSchema) {
		this.indexSchema = indexSchema;
	}

	public String getTargetFeature() {
		return targetFeature;
	}

	public void setTargetFeature(String targetFeature) {
		this.targetFeature = targetFeature;
	}

	public String getTargetFrequency() {
		return targetFrequency;
	}

	public void setTargetFrequency(String targetFrequency) {
		this.targetFrequency = targetFrequency;
	}
}
