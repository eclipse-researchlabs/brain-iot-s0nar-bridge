package com.improvingmetrics.brain.iot.s0nar.service;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class ModelDTO extends DTO {
	@SerializedName("_id")
	private String id;
	
	@SerializedName("bin_s3_uri")
	private String binS3Uri;
	
	@SerializedName("creation_date")
	private String creationDate;
	
	@SerializedName("dataset")
	private String dataSet;
	
	@SerializedName("index_feature")
	private String indexFeature;
	
	@SerializedName("index_schema")
	private String indexSchema;
	
	private ModelStatus status;
	
	@SerializedName("target_feature")
	private String targetFeature;
	
	private String type;
	
	@SerializedName("raised_exception")
	private String raisedException;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBinS3Uri() {
		return binS3Uri;
	}

	public void setBinS3Uri(String binS3Uri) {
		this.binS3Uri = binS3Uri;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getDataSet() {
		return dataSet;
	}

	public void setDataSet(String dataSet) {
		this.dataSet = dataSet;
	}

	public String getIndexFeature() {
		return indexFeature;
	}

	public void setIndexFeature(String indexFeature) {
		this.indexFeature = indexFeature;
	}

	public String getIndexSchema() {
		return indexSchema;
	}

	public void setIndexSchema(String indexSchema) {
		this.indexSchema = indexSchema;
	}

	public ModelStatus getStatus() {
		return status;
	}

	public void setStatus(ModelStatus status) {
		this.status = status;
	}

	public String getTargetFeature() {
		return targetFeature;
	}

	public void setTargetFeature(String targetFeature) {
		this.targetFeature = targetFeature;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRaisedException() {
		return raisedException;
	}

	public void setRaisedException(String raisedException) {
		this.raisedException = raisedException;
	}	
}
