package com.improvingmetrics.brain.iot.s0nar.service;

import com.google.gson.annotations.SerializedName;

public enum ModelType {
	@SerializedName("ARIMA")
	ARIMA,
	
	@SerializedName("LSTM_CPU")
	LSTM_CPU,
	
	@SerializedName("LSTM_GPU")
	LSTM_GPU
}
