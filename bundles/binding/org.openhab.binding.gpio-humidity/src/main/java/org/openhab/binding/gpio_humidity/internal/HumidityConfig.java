package org.openhab.binding.gpio_humidity.internal;

import org.openhab.io.gpio_raspberry.device.I2CConfig;

public class HumidityConfig extends I2CConfig {

	public HumidityConfig(String id) {
		super(id);
	}

	public HumidityConfig(String id, byte address) {
		super(id, address);
	}
	
}
