package com.improvingmetrics.brain.iot.s0nar.service;

import java.util.List;

import org.osgi.dto.DTO;

import com.google.gson.annotations.SerializedName;

public class AnomaliesReportDTO extends DTO {
	@SerializedName(value = "_id")
	private String id;
	
	@SerializedName(value = "creation_date")
	private String creationDate;
	
	@SerializedName(value = "model")
	private String modelId;
	
	private List<AnomalyDTO> anomalies;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public List<AnomalyDTO> getAnomalies() {
		return anomalies;
	}

	public void setAnomalies(List<AnomalyDTO> anomalies) {
		this.anomalies = anomalies;
	}
}
