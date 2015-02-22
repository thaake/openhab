package org.openhab.binding.gpio_humidity.internal;

import org.openhab.core.items.Item;
import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;

public class HumidityItemConfig extends GpioI2CItemConfig {
	private Long wet;
	private Long dry;
	private Byte port;
	private int measures;

	public HumidityItemConfig() {
		super();
	}

	public HumidityItemConfig(Item item, Byte port, Long wet, Long dry, int measures) {
		super(item);
		this.wet = wet;
		this.dry = dry;
		this.measures = measures;
		this.port = port;
	}

	public Long getWet() {
		return wet;
	}

	public void setWet(Long wet) {
		this.wet = wet;
	}

	public Long getDry() {
		return dry;
	}

	public void setDry(Long dry) {
		this.dry = dry;
	}

	public int getMeasures() {
		return measures;
	}

	public void setMeasures(int measures) {
		this.measures = measures;
	}

	public Byte getPort() {
		return port;
	}

	public void setPort(Byte port) {
		this.port = port;
	}
	
	
}
