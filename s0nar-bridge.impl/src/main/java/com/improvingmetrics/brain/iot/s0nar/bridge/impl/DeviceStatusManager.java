package com.improvingmetrics.brain.iot.s0nar.bridge.impl;

import java.util.HashMap;
import java.util.Map;

public class DeviceStatusManager {
	private Map<String, DeviceStatus> deviceStatusMapping;
	
	public DeviceStatusManager() {
		this.deviceStatusMapping = new HashMap<String, DeviceStatus>();
	}
	
	public void configure(String mapping) {
		if (mapping != null && !mapping.isEmpty()) {
			String[] relations = mapping.split(",");
			
			for (String relation : relations) {
				String[] relationComponents = relation.split(":");
				
				this.deviceStatusMapping.put(
					relationComponents[0],
					new DeviceStatus(relationComponents[1])
				);
			}
		}
	}
	
	public String getDataSetIdForDevice(String deviceId) {
		DeviceStatus deviceStatus = this.deviceStatusMapping.get(deviceId);
		
		return deviceStatus != null? deviceStatus.getDataSetId() : null;
	}
	
	public void setDataSetIdForDevice(String deviceId, String dataSetId) {
		this.deviceStatusMapping.put(deviceId, new DeviceStatus(dataSetId));
	}
	
	public long getLastAnomalyTSForDevice(String deviceId) {
		DeviceStatus deviceStatus = this.deviceStatusMapping.get(deviceId);
		
		return deviceStatus != null? deviceStatus.getLastAnomalyTS() : 0l;
	}
	
	public void setLastAnomalyTSForDevice(String deviceId, long lastAnomalyTS) {
		DeviceStatus deviceStatus = this.deviceStatusMapping.get(deviceId);
		
		deviceStatus.setLastAnomalyTS(lastAnomalyTS);
	}
}
