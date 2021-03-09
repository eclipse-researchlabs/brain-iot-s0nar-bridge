package com.improvingmetrics.brain.iot.s0nar.service;

import com.google.gson.annotations.SerializedName;

public enum ModelStatus {
	@SerializedName("INACTIVE")
	INACTIVE,
	
	@SerializedName("TRAINING")
	TRAINING,
	
	@SerializedName("FAILED")
	FAILED,
	
	@SerializedName("FINISHED")
	FINISHED
}
