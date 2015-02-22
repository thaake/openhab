package org.openhab.io.gpio_raspberry.item;


public class GpioIOItemConfig extends GpioItemConfig {
	private String itemName;
	private boolean in = false;
	private boolean activeLow = false;
	private byte port;
	private int debounce;
	
	public GpioIOItemConfig(String itemName, byte port, boolean in, boolean activeLow, int debounce) {
		super();
		this.itemName = itemName;
		this.in = in;
		this.activeLow = activeLow;
		this.port = port;
		this.debounce = debounce;
	}

	public GpioIOItemConfig() {
		super();
	}
	
	public boolean isIn() {
		return in;
	}

	public void setIn(boolean in) {
		this.in = in;
	}

	public boolean isActiveLow() {
		return activeLow;
	}

	public void setActiveLow(boolean activeLow) {
		this.activeLow = activeLow;
	}
	
	public byte getPort() {
		return port;
	}

	public void setPort(byte port) {
		this.port = port;
	}
	
	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	public int getDebounce() {
		return debounce;
	}

	public void setDebounce(int debounce) {
		this.debounce = debounce;
	}

	@Override
	public String toString() {
		return "GpioIOItemConfig [in=" + in + ", activeLow=" + activeLow
				+ ", item=" + item + ", id=" + id + ", refresh="
				+ refresh + ", port=" + port + ", lastRefresh=" + lastRefresh
				+ "]";
	}
	
	
}
