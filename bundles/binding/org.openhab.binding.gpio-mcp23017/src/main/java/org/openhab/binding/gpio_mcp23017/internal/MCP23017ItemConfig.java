package org.openhab.binding.gpio_mcp23017.internal;

import org.openhab.core.items.Item;
import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;

public class MCP23017ItemConfig extends GpioI2CItemConfig {
	private byte port;
	private char bank;
	private boolean in;

	public MCP23017ItemConfig() {
		super();
	}
	
	public MCP23017ItemConfig(Item item, byte port, char bank, boolean in) {
		super(item);
		this.port = port;
		this.bank = bank;
		this.in = in;
	}

	public byte getPort() {
		return port;
	}

	public void setPort(byte port) {
		this.port = port;
	}

	public char getBank() {
		return bank;
	}

	public void setBank(char bank) {
		this.bank = bank;
	}

	public boolean isIn() {
		return in;
	}

	public void setIn(boolean in) {
		this.in = in;
	}
	
	
}
