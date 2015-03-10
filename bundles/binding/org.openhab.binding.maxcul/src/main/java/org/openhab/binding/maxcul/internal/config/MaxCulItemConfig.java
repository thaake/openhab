package org.openhab.binding.maxcul.internal.config;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;

public abstract class MaxCulItemConfig implements BindingConfig {
	private Item item;

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}
	
	
}
