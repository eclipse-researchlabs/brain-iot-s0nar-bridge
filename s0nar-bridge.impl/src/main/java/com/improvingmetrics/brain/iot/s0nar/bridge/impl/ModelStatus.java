package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

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
