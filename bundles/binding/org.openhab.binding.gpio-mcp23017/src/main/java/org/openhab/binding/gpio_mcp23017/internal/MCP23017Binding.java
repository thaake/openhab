/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_mcp23017.internal;

import java.util.Dictionary;

import org.openhab.binding.gpio_mcp23017.MCP23017BindingProvider;

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
public class MCP23017Binding extends AbstractActiveBinding<MCP23017BindingProvider> implements ManagedService {

	private static final Logger logger = 
		LoggerFactory.getLogger(MCP23017Binding.class);
	
	private static final String PROP_ADDRESS = "address";
	
	private long minimumRefresh = 5000;
	
	private GpioLoader gpioLoader;
	private ItemRegistry itemRegistry;
	
	private MCP23017Device device;

	
	public MCP23017Binding() {
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
		this.device.stopPolling();
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
		
		for (MCP23017BindingProvider provider : providers) {
			
			MCP23017ItemConfig itemConfig = provider.getItemConfig(itemName);
			Item item = null;
			try {
				item = this.itemRegistry.getItem(itemName);
			} catch (ItemNotFoundException e) {
				logger.error("cannot find item: " + itemName);
				return;
			}
			State state = device.communicate(command, itemConfig, item.getState());
			if (state == null) {
				logger.debug("no state returned, do not publish");
				continue;
			}
			
			super.eventPublisher.postUpdate(itemName, state);
		}
	}
	
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties != null) {
			logger.info("loading configuration");
			byte address = Byte.parseByte((String) properties.get(PROP_ADDRESS), 16);
			String id = this.getName();
			
			logger.debug("id: " + id);
			logger.debug("address: " + address);
			
			MCP23017Config config = new MCP23017Config(id, address);
			
			try {
				this.device = (MCP23017Device) this.gpioLoader.createI2CDevice(config, MCP23017Device.class);
			} catch (GpioException e) {
				logger.error(e.getMessage());
			}
			
			setProperlyConfigured(true);
		}
	}


	@Override
	protected void execute() {
		for (MCP23017BindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				MCP23017ItemConfig itemConfig = provider.getItemConfig(itemName);
				if (itemConfig.isIn()) {
					this.device.startPolling(itemConfig, eventPublisher);
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
		return "GPIO-MCP23017 Service";
	}
}
