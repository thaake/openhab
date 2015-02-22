package org.openhab.binding.gpio_mcp23017.internal;

import org.openhab.io.gpio_raspberry.device.I2CConfig;

public class MCP23017Config extends I2CConfig {

	public MCP23017Config(String id) {
		super(id);
	}

	public MCP23017Config(String id, byte address) {
		super(id, address);
	}
	
}
