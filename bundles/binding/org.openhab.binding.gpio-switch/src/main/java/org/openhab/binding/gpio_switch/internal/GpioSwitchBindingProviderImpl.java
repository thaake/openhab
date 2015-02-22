/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_switch.internal;

import org.openhab.binding.gpio_switch.GpioSwitchBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.io.gpio_raspberry.item.GpioIOItemConfig;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Robert Delbrück
 * @since 1.6.0
 */
public class GpioSwitchBindingProviderImpl extends AbstractGenericBindingProvider implements GpioSwitchBindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(GpioSwitchBindingProviderImpl.class);
	
	private static final String PROP_IN = "in";
	private static final String PROP_ACTIVE_LOW = "activeLow";
	private static final String PROP_PORT = "port";
	private static final String PROP_DEBOUNCE = "debounce";
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "gpioSwitch";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only SwitchItem are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		logger.debug("reading item: " + item);
		
		boolean in = false;
		boolean activeLow = false;
		byte port = -1;
		int debounce = 0;
		
		String[] configParts = bindingConfig.split(" ");
		for (String configPart : configParts) {
			String[] configPartSplit = configPart.split(":");
			if (configPartSplit[0].equals(PROP_IN)) {
				in = Boolean.parseBoolean(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_ACTIVE_LOW)) {
				activeLow = Boolean.parseBoolean(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_PORT)) {
				port = Byte.parseByte(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_DEBOUNCE)) {
				debounce = Integer.parseInt(configPartSplit[1]);
			}
		}
		
		logger.debug("in: " + in);
		logger.debug("activeLow: " + activeLow);
		logger.debug("port: " + port);
		
		if (port == -1) {
			logger.error("port is not configured, but required");
			return;
		}
		
		super.bindingConfigs.put(item.getName(), new GpioIOItemConfig(item.getName(), port, in, activeLow, debounce));
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public GpioIOItemConfig getItemConfig(String itemName) {
		return (GpioIOItemConfig) super.bindingConfigs.get(itemName);
	}
	
	
}
