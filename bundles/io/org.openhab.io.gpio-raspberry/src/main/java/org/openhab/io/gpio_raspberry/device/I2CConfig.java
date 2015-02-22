package org.openhab.io.gpio_raspberry.device;


public class I2CConfig extends DeviceConfig {
	private byte address;

	public I2CConfig(String id) {
		super(id);
	}

	public I2CConfig(String id, byte address) {
		super(id);
		this.address = address;
	}

	public byte getAddress() {
		return address;
	}

	public void setAddress(byte address) {
		this.address = address;
	}
}
