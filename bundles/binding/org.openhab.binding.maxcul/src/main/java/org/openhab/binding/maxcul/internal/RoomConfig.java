package org.openhab.binding.maxcul.internal;

import java.util.ArrayList;
import java.util.List;

public class RoomConfig {
	private String name;
	private List<String> deviceList = new ArrayList<String>();
	private int group;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<String> getDeviceList() {
		return deviceList;
	}
	
	public void setDeviceList(List<String> deviceList) {
		this.deviceList = deviceList;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	public boolean isInRoom(String serialNumber) {
		return this.deviceList.contains(serialNumber);
	}

	public List<String> getOtherDevices(String serialNumber) {
		List<String> otherDevices = new ArrayList<String>();
		for (String device : this.deviceList) {
			if (device.equals(serialNumber)) {
				continue;
			}
			otherDevices.add(device);
		}
		return otherDevices;
	}
	
	
}
