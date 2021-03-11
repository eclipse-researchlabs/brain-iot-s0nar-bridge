package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

public class DeviceStatus {
	private String dataSetId;
	private long lastAnomalyTS = 0l;
	private long lastManagedEventTS = 0l;
	
	public DeviceStatus() {}
	
	public DeviceStatus(String dataSetId) {
		this.dataSetId = dataSetId;
	}
	
	public String getDataSetId() {
		return dataSetId;
	}
	
	public void setDataSetId(String dataSetId) {
		this.dataSetId = dataSetId;
	}
	
	public long getLastAnomalyTS() {
		return lastAnomalyTS;
	}
	
	public void setLastAnomalyTS(long lastAnomalyTS) {
		this.lastAnomalyTS = lastAnomalyTS;
	}

	public long getLastManagedEventTS() {
		return lastManagedEventTS;
	}

	public void setLastManagedEventTS(long lastManagedEventTS) {
		this.lastManagedEventTS = lastManagedEventTS;
	}
}
