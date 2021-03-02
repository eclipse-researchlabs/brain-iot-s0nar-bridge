package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class DataSetDTO extends DTO {
	@SerializedName("_id")
	private String id;
	
	private DataSetDescriptorsDTO descriptors;
	
	private String name;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DataSetDescriptorsDTO getDescriptors() {
		return descriptors;
	}

	public void setDescriptors(DataSetDescriptorsDTO descriptors) {
		this.descriptors = descriptors;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
