package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class DataSetDTO extends DTO {
	@SerializedName("_id")
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
