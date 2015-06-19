/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_mcp23017.internal;

import org.openhab.binding.gpio_mcp23017.MCP23017BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.SwitchItem;
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
public class MCP23017BindingProviderImpl extends AbstractGenericBindingProvider implements MCP23017BindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(MCP23017BindingProviderImpl.class);
	
	private static final String PROP_PORT = "port";
	private static final String PROP_BANK = "bank";
	private static final String PROP_IN = "in";
	private static final String PROP_POLL_INTERVAL = "pollInterval";
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "gpioMCP23017";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof ContactItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Switch-, or ContactItem are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		Byte port = null;
		Character bank = null;
		boolean in = false;
		int pollInterval = 100;
		
		String[] configParts = bindingConfig.split(" ");
		for (String configPart : configParts) {
			String[] configPartSplit = configPart.split(":");
			if (configPartSplit[0].equals(PROP_PORT)) {
				port = Byte.parseByte(configPartSplit[1]);
			} else if (configPartSplit[0].equals(PROP_BANK)) {
				bank = (configPartSplit[1]).charAt(0);
			} else if (configPartSplit[0].equals(PROP_IN)) {
				in = Boolean.parseBoolean(configPartSplit[1] + "");
			} else if (configPartSplit[0].equals(PROP_POLL_INTERVAL)) {
				pollInterval = Integer.parseInt(configPartSplit[1] + "");
			}
		}
		
		logger.debug("port: " + port);
		
		if (port == null) {
			logger.error("port is not configured, but required");
			return;
		}
		if (bank == null) {
			logger.error("bank is not configured, but required");
			return;
		}
		
		logger.debug("reading item: " + item);
		super.bindingConfigs.put(item.getName(), new MCP23017ItemConfig(item, port, bank, in, pollInterval));
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public MCP23017ItemConfig getItemConfig(String itemName) {
		return (MCP23017ItemConfig) super.bindingConfigs.get(itemName);
	}
	
	
}
