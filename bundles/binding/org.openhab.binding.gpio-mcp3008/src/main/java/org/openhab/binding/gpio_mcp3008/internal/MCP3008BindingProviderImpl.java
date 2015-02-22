/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_mcp3008.internal;

import org.openhab.binding.gpio_mcp3008.MCP3008BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
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
public class MCP3008BindingProviderImpl extends AbstractGenericBindingProvider implements MCP3008BindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(MCP3008BindingProviderImpl.class);
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "gpioMCP3008";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof NumberItem || item instanceof DimmerItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Number-, or DimmerItem are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
//		if (bindingConfig == null) {
//			logger.debug("binding-configuration is currently not set for item: " + item.getName());
//		}
		
		logger.debug("reading item: " + item);
		super.bindingConfigs.put(item.getName(), new MCP3008ItemConfig());
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public MCP3008ItemConfig getItemConfig(String itemName) {
		return (MCP3008ItemConfig) super.bindingConfigs.get(itemName);
	}
	
	
}
