/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.lirc.internal;

import org.openhab.binding.lirc.LircBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class LircGenericBindingProvider.
 *
 * @author Robert Delbrück
 * @since 1.6.1
 * 
 */


public class LircGenericBindingProvider extends AbstractGenericBindingProvider implements LircBindingProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(LircGenericBindingProvider.class);
	
	private static final String PROP_COMMAND = "command";
	private static final String PROP_DEVICE = "device";
	
    static int counter = 0;
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "lirc";
	}
	
	
	/**
	 * device:RECEIVER command:ON
	 */
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		String command = null;
		String device = null;
		
		String[] configItems = bindingConfig.split(" ");
		for (String configItem : configItems) {
			String[] keyValues = configItem.split(":");
			if (keyValues[0].equals(PROP_COMMAND)) {
				command = keyValues[1];
			} else if (keyValues[0].equals(PROP_DEVICE)) {
				device = keyValues[1];
			}
		}
		
		LircBindingConfig config = new LircBindingConfig(device, command);
		logger.debug("adding config: {}", config);
		
		super.addBindingConfig(item, config);
	}

	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		
	}

	@Override
	public LircBindingConfig getConfig(String itemName) {
		return (LircBindingConfig) bindingConfigs.get(itemName);
	}

}