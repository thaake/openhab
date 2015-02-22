package org.openhab.binding.gpio_pcf8591.internal;

import org.openhab.core.items.Item;
import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;

public class PCF8591ItemConfig extends GpioI2CItemConfig {
	private boolean in;
	private int port;

	public PCF8591ItemConfig(Item item, boolean in, int port) {
		super(item);
		this.in = in;
		this.port = port;
	}

	public boolean isIn() {
		return in;
	}

	public void setIn(boolean in) {
		this.in = in;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
	
}
