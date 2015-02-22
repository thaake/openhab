package org.openhab.io.gpio_raspberry.item;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;

public class GpioItemConfig implements BindingConfig {
	protected Item item;
	public String id;
	public long refresh = -1;
	
	// runtime
	public long lastRefresh = 0;

	public GpioItemConfig() {
		super();
	}

	public GpioItemConfig(Item item) {
		super();
		this.item = item;
	}
	
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@Override
	public String toString() {
		return "GpioItemConfig [item=" + item + ", id=" + id
				+ ", refresh=" + refresh + ", lastRefresh="
				+ lastRefresh + "]";
	}
}
