/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_humidity.internal;

import java.util.Dictionary;

import org.openhab.binding.gpio_humidity.HumidityBindingProvider;

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
public class HumidityBinding extends AbstractActiveBinding<HumidityBindingProvider> implements ManagedService {

	private static final Logger logger = 
		LoggerFactory.getLogger(HumidityBinding.class);
	
	private static final String PROP_ADDRESS = "address";
	private static final String PROP_REFERSH_INTERVAL = "refreshInterval";
	
	private long minimumRefresh = 5000;
	
	private GpioLoader gpioLoader;
	private ItemRegistry itemRegistry;
	
	private HumidityDevice device;

	
	public HumidityBinding() {
	}
	
	public void setGpioLoader(GpioLoader gpioLoader) {
		this.gpioLoader = gpioLoader;
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
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}


	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties == null) {
			logger.warn("no configuration found");
		} else {
			this.minimumRefresh = Integer.parseInt("" + (properties.get(PROP_REFERSH_INTERVAL) != null ? (properties.get(PROP_REFERSH_INTERVAL) + "") : 1000*60*5));
			
			byte address = Byte.parseByte((String) properties.get(PROP_ADDRESS), 16);
			String id = this.getName();
			
			logger.debug("id: " + id);
			logger.debug("address: " + address);
			
			HumidityConfig config = new HumidityConfig(id, address);
			
			try {
				this.device = (HumidityDevice) this.gpioLoader.createI2CDevice(config, HumidityDevice.class);
			} catch (GpioException e) {
				logger.error(e.getMessage());
			}
			
			setProperlyConfigured(true);
		}
	}


	@Override
	protected void execute() {
		if (providers.size() == 0) {
			logger.debug("no providers are set");
		}
		for (HumidityBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				HumidityItemConfig itemConfig = provider.getItemConfig(itemName);
				Item item = null;
				try {
					item = this.itemRegistry.getItem(itemName);
				} catch (ItemNotFoundException e) {
					logger.error("cannot find item: " + itemName);
					return;
				}
				try {
					State state = device.communicate(null, itemConfig, item.getState());
					if (state == null) {
						logger.debug("no state returned, do not publish");
						continue;
					}
					
					super.eventPublisher.postUpdate(itemName, state);
				} catch (Throwable e) {
					logger.error("error reading data", e);
				}
			}
		}
	}


	@Override
	protected long getRefreshInterval() {
		return minimumRefresh;
	}


	@Override
	protected String getName() {
		return "GPIO Humidity Service";
	}
}
