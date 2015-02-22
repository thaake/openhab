/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_pcf8591.internal;

import java.util.Dictionary;

import org.apache.commons.lang.math.NumberUtils;
import org.openhab.binding.gpio_pcf8591.PCF8591BindingProvider;

import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.gpio_raspberry.GpioException;
import org.openhab.io.gpio_raspberry.GpioLoader;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author Robert Delbrück
 * @since 1.6.0
 */
public class PCF8591Binding extends AbstractActiveBinding<PCF8591BindingProvider> implements ManagedService {

	private static final Logger logger = 
		LoggerFactory.getLogger(PCF8591Binding.class);
	
	private static final String PROP_ADDRESS = "address";
	private static final String PROP_REFRESH_INTERVAL = "refreshInterval";
	private static final int DEFAULT_REFRESH_INTERVAL = 60000;
	
	private long minimumRefresh = -1;
	
	private GpioLoader gpioLoader;
	private ItemRegistry itemRegistry;
	
	private PCF8591Device device;

	
	public PCF8591Binding() {
	}
	
	public void setGpioLoader(GpioLoader gpioLoader) {
		this.gpioLoader = gpioLoader;
		logger.trace("setting gpioLoader: {}", this.gpioLoader);
	}
	
	public void unsetGpioLoader(GpioLoader gpioLoader) {
		this.gpioLoader = null;
	}
	
	public void setItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = itemRegistry;
	}
	
	public void unsetItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = null;
	}
	
	public void activate() {
		super.activate();
	}
	
	public void deactivate() {
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}

	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		for (PCF8591BindingProvider provider : providers) {
			PCF8591ItemConfig itemConfig = provider.getItemConfig(itemName);
			Item item = null;
			try {
				item = this.itemRegistry.getItem(itemName);
			} catch (ItemNotFoundException e) {
				logger.error("cannot find item: " + itemName);
				return;
			}
			logger.trace("found item: {}", item);
			if (device == null) {
				logger.error("device is not set");
				return;
			}
			State state = device.communicate(command, itemConfig, item.getState());
			if (state == null) {
				logger.debug("no state returned, do not publish");
				continue;
			}
			logger.debug("returned state: {}", state);
			
			super.eventPublisher.postUpdate(itemName, state);
		}
	}
	
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties == null) {
			logger.warn("no configuration found");
		} else {
			this.minimumRefresh = NumberUtils.toLong(properties.get(PROP_REFRESH_INTERVAL) + "", DEFAULT_REFRESH_INTERVAL);
			byte address = Byte.parseByte((String) properties.get(PROP_ADDRESS), 16);
			String id = this.getName();
			
			logger.debug("id: " + id);
			logger.debug("address: " + address);
			
			PCF8591Config config = new PCF8591Config(id, address);
			
			try {
				this.device = (PCF8591Device) this.gpioLoader.createI2CDevice(config, PCF8591Device.class);
			} catch (GpioException e) {
				logger.error(e.getMessage());
			} catch (Exception e) {
				logger.error(e.getLocalizedMessage(), e);
			}
			
			setProperlyConfigured(true);
		}
	}


	@Override
	protected void execute() {
		PCF8591BindingProvider provider = null;
		for (PCF8591BindingProvider provider_ : providers) {
			provider = provider_;
		}
		
		if (provider == null) {
			logger.warn("no providers found");
		}
		
		for (String itemName : provider.getItemNames()) {
			PCF8591ItemConfig itemConfig = provider.getItemConfig(itemName);
			if (itemConfig.isIn()) {
				Item item = null;
				try {
					item = this.itemRegistry.getItem(itemName);
				} catch (ItemNotFoundException e) {
					logger.error("cannot find item: " + itemName);
					return;
				}
				logger.trace("found item: {}", item);
				if (device == null) {
					logger.error("device is not set");
					return;
				}
				State state = device.communicate(null, itemConfig, item.getState());
				if (state == null) {
					logger.debug("no state returned, do not publish");
					continue;
				}
				logger.debug("returned state: {}", state);
				
				super.eventPublisher.postUpdate(itemName, state);
			}
		}
	}


	@Override
	protected long getRefreshInterval() {
		return minimumRefresh;
	}


	@Override
	protected String getName() {
		return "GPIO PCF8591 Service";
	}
}
