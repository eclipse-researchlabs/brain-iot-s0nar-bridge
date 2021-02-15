package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import com.google.gson.annotations.SerializedName;

public class UpdateDatasetResponseDTO {
	@SerializedName(value = "_id")
	public String id;
	
	@SerializedName(value = "creation_date")
	public String creationDate;
	
	public String name;
	
	@SerializedName(value = "s3_uri")
	public String s3Uri;
}
