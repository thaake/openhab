package org.openhab.binding.gpio_mcp23017.internal;

import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;

public class MCP23017ItemConfig extends GpioI2CItemConfig {
	private byte port;
	private char bank;
	private String itemName;
	private boolean in;

	public MCP23017ItemConfig() {
		super();
	}
	
	public MCP23017ItemConfig(byte port, char bank, String itemName, boolean in) {
		super();
		this.port = port;
		this.bank = bank;
		this.itemName = itemName;
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

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public boolean isIn() {
		return in;
	}

	public void setIn(boolean in) {
		this.in = in;
	}
	
	
}
