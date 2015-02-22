/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpio_pcf8591.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.openhab.binding.gpio_pcf8591.PCF8591BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
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
public class PCF8591BindingProviderImpl extends AbstractGenericBindingProvider implements PCF8591BindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(PCF8591BindingProviderImpl.class);
	
	private static final String PARAM_IN = "in:([^ ]+)";
	private static final String PARAM_PORT = "port:([^ ]+)";
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "gpioPCF8591";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof DimmerItem || item instanceof SwitchItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Dimmer- and SwitchItem are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		logger.debug("reading item: " + item);
		
		String inString = null;
		Matcher mIn = Pattern.compile(PARAM_IN).matcher(bindingConfig);
		if (mIn.find()) {
			inString = mIn.group(1);
		}
		boolean in = BooleanUtils.isTrue(BooleanUtils.toBooleanObject(inString));
		
		
		String portString = null;
		Matcher mPort = Pattern.compile(PARAM_PORT).matcher(bindingConfig);
		if (mPort.find()) {
			portString = mPort.group(1);
		}
		int port = NumberUtils.toInt(portString, -1);
		
		if (in && port == -1) {
			throw new BindingConfigParseException("parameter port is required, if in:true");
		}
		
		super.bindingConfigs.put(item.getName(), new PCF8591ItemConfig(item, in, port));
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public PCF8591ItemConfig getItemConfig(String itemName) {
		return (PCF8591ItemConfig) super.bindingConfigs.get(itemName);
	}
	
	
}
