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
	
	private DeviceStatus getDeviceStatus(String deviceId) {
		DeviceStatus deviceStatus = this.deviceStatusMapping.get(deviceId);
		
		if (deviceStatus != null){
			return deviceStatus;
		} else {
			DeviceStatus newDeviceStatus = new DeviceStatus();
			this.deviceStatusMapping.put(deviceId, newDeviceStatus);
			
			return newDeviceStatus;
		}
	}
	
	public String getDataSetIdForDevice(String deviceId) {
		return this.getDeviceStatus(deviceId).getDataSetId();
	}
	
	public void setDataSetIdForDevice(String deviceId, String dataSetId) {
		this.getDeviceStatus(deviceId).setDataSetId(dataSetId);
	}
	
	public long getLastAnomalyTSForDevice(String deviceId) {
		return this.getDeviceStatus(deviceId).getLastAnomalyTS();
	}
	
	public void setLastAnomalyTSForDevice(String deviceId, long lastAnomalyTS) {
		this.getDeviceStatus(deviceId).setLastAnomalyTS(lastAnomalyTS);
	}
	
	public long getLastManagedEventTSForDevice(String deviceId) {
		return this.getDeviceStatus(deviceId).getLastManagedEventTS();
	}
	
	public void setLastManagedEventTSForDevice(String deviceId, long newManagedEventTS) {
		this.getDeviceStatus(deviceId).setLastManagedEventTS(newManagedEventTS);
	}
	
	public void resetLastManagedEventTSFromDevice(String deviceId) {
		this.setLastManagedEventTSForDevice(deviceId, System.currentTimeMillis());
	}
	
	public long getSecsFromLastManagedEventForDevice(String deviceId) {
		return (System.currentTimeMillis() - this.getLastManagedEventTSForDevice(deviceId)) / 1000;
	}
}
