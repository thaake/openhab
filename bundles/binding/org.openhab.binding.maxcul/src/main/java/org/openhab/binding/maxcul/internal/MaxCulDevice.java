/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.maxcul.internal;

/**
 * Define device types
 * 
 * @author Paul Hampson (cyclingengineer)
 * @since 1.6.0
 */
public enum MaxCulDevice {
	RADIATORTHERMOSTAT(1), RADIATORTHERMOSTATPLUS(2), 
	WALLTHERMOSTAT(3), SHUTTERCONTACT(4), PUSHBUTTON(5);

	private final int devType;

	private MaxCulDevice(int idx) {
		devType = idx;
	}

	public int getDeviceTypeInt() {
		return devType;
	}

	public static MaxCulDevice getDeviceTypeFromInt(int idx) {
		for (int i = 0; i < MaxCulDevice.values().length; i++) {
			if (MaxCulDevice.values()[i].getDeviceTypeInt() == idx)
				return MaxCulDevice.values()[i];
		}
		return null;
	}

	public static MaxCulDevice parse(String val) {
		for (MaxCulDevice device : values()) {
			if (device.name().equalsIgnoreCase(val)) {
				return device;
			}
		}
		return null;
	}
}
