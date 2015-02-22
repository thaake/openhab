/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_humidity.internal;

import org.openhab.binding.gpio_humidity.HumidityBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
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
public class HumidityBindingProviderImpl extends AbstractGenericBindingProvider implements HumidityBindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(HumidityBindingProviderImpl.class);
	
	private static final String PROP_WET = "wet";
	private static final String PROP_DRY = "dry";
	private static final String PROP_MEASURES = "measures";
	private static final String PROP_PORT = "port";
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "gpioHumidity";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof NumberItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only NumberItem are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		logger.debug("reading item: " + item);
		
		Long wet = null;
		Long dry = null;
		int measures = 1;
		Byte port = null;
		
		String[] configParts = bindingConfig.split(" ");
		for (String configPart : configParts) {
			String[] configPartSplit = configPart.split(":");
			if (configPartSplit[0].equals(PROP_WET)) {
				wet = Long.parseLong(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_DRY)) {
				dry = Long.parseLong(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_MEASURES)) {
				measures = Integer.parseInt(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_PORT)) {
				port = Byte.parseByte(configPartSplit[1]);
			}
		}
		
		logger.debug("wet: " + wet);
		logger.debug("dry: " + dry);
		logger.debug("measures: " + measures);
		logger.debug("port: " + port);
		
		if (port == null) {
			logger.error("port is not configured, but required");
			return;
		}
		
		super.bindingConfigs.put(item.getName(), new HumidityItemConfig(item, port, wet, dry, measures));
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public HumidityItemConfig getItemConfig(String itemName) {
		return (HumidityItemConfig) super.bindingConfigs.get(itemName);
	}
	
	
}
