package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import com.google.gson.annotations.SerializedName;

public class ModelDTO {
	@SerializedName(value = "_id")
	public String id;
	
	@SerializedName(value = "bin_s3_uri")
	public String binS3Uri;
	
	@SerializedName(value = "creation_date")
	public String creationDate;
	
	@SerializedName(value = "dataset")
	public String dataSet;
	
	@SerializedName(value = "index_feature")
	public String indexFeature;
	
	@SerializedName(value = "index_schema")
	public String indexSchema;
	
	public ModelStatus status;
	
	@SerializedName(value = "target_feature")
	public String targetFeature;
	
	public String type;	
}
