/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.speech.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.speech.SpeechBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * speech="primaryName:'Tischlampe' alternativeNames:'Dekolampe, Stehlampe' commands:'einschalten>ON'"
 * 
 * @author Robert Delbrück
 * @since 1.7.0
 */
public class SpeechBindingProviderImpl extends AbstractGenericBindingProvider implements SpeechBindingProvider {
	private static final Logger logger = LoggerFactory.getLogger(SpeechBindingProviderImpl.class);
	
	private static final String REGEX_PRIMARY_NAME = "primaryName:'([^']*)'";
	private static final String REGEX_ALTERNATIVE_NAMES = "alternativeName:'([^']*)'";
	private static final String REGEX_COMMANDS = "command:'([^']*)'";
	private static final String REGEX_TYPE = "type:'([^']*)'";
	
	private ItemRegistry itemRegistry;
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "speech";
	}
	
	public void setItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = itemRegistry;
	}

	public void unsetItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = null;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
//		if (!(item instanceof StringItem && item.getName().equals("VoiceCommand"))) {
//			throw new BindingConfigParseException("item '" + item.getName()
//					+ "' is of type '" + item.getClass().getSimpleName()
//					+ "', only StringItem are allowed - please check your *.items configuration");
//		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		logger.debug("reading item: " + item);
		
		if (item.getName().equals("VoiceCommand")) {
			logger.info("found VoiceCommand Item");
			super.addBindingConfig(item, new SpeechInItemConfig());
			return;
		}
		
		String primaryName = null;
		Matcher matcher1 = Pattern.compile(REGEX_PRIMARY_NAME).matcher(bindingConfig);
		if (matcher1.find()) {
			primaryName = matcher1.group(1).toLowerCase();
		}
		
		List<String> alternativeNameList = new ArrayList<String>();
		Matcher matcher2 = Pattern.compile(REGEX_ALTERNATIVE_NAMES).matcher(bindingConfig);
		while (matcher2.find()) {
			alternativeNameList.add(matcher2.group(1).toLowerCase());
		}
		
		Map<String, String> commandList = new HashMap<String, String>();
		Matcher matcher3 = Pattern.compile(REGEX_COMMANDS).matcher(bindingConfig);
		while (matcher3.find()) {
			String command = matcher3.group(1);
			String[] split = command.split(">");
			if (split.length != 2) {
				throw new BindingConfigParseException("command is in wrong format");
			}
			commandList.put(split[0].trim().toLowerCase(), split[1].trim());
		}
		
		String typeString = null;
		Matcher matcher4 = Pattern.compile(REGEX_TYPE).matcher(bindingConfig);
		if (matcher4.find()) {
			typeString = matcher4.group(1);
		}
		
		if (primaryName == null) {
			throw new BindingConfigParseException("cannot find required parameter 'primaryName'");
		}
		
		if (typeString == null) {
			throw new BindingConfigParseException("cannot find required parameter 'type'");
		}
		Type type = Type.valueOf(typeString);
		if (type == null) {
			throw new BindingConfigParseException("cannot parse required parameter 'type': " + typeString);
		}
		
		SpeechCommandItemConfig config = new SpeechCommandItemConfig(item, primaryName, alternativeNameList);
		config.setCommands(commandList);
		config.setType(type);
		logger.debug("found config for item '{}': {}", item, config);
		super.addBindingConfig(item, config);
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		return super.bindingConfigs.get(itemName) != null;
	}

	@Override
	public SpeechItemConfig getItemConfig(String itemName) {
		return (SpeechItemConfig) super.bindingConfigs.get(itemName);
	}
	
	@Override
	public List<SpeechCommandItemConfig> getAllItemCommandConfigs() {
		List<SpeechCommandItemConfig> list = new ArrayList<SpeechCommandItemConfig>();
		for (BindingConfig bindingConfig : this.bindingConfigs.values()) {
			if (bindingConfig instanceof SpeechCommandItemConfig) {
				list.add((SpeechCommandItemConfig) bindingConfig);
			}
		}
		return list;
	}

	@Override
	public List<SpeechCommandItemConfig> getItemConfigs(List<Item> itemList) {
		List<SpeechCommandItemConfig> list = new ArrayList<SpeechCommandItemConfig>();
		for (Item item : itemList) {
			SpeechItemConfig c = getItemConfig(item.getName());
			
			if (c != null && c instanceof SpeechCommandItemConfig) {
				list.add((SpeechCommandItemConfig) c);
			}
		}
		return list;
	}

	@Override
	public List<SpeechCommandItemConfig> getAllItemCommandConfigsForGroups(
			SpeechCommandItemConfig function, SpeechCommandItemConfig place) {
		List<SpeechCommandItemConfig> list = new ArrayList<SpeechCommandItemConfig>();
		
		for (SpeechCommandItemConfig deviceConfig : this.getAllItemCommandConfigs()) {
			if (deviceConfig.getType() != Type.DEVICE) {
				continue;
			}
			try {
				List<String> groups = itemRegistry.getItem(deviceConfig.getItem().getName()).getGroupNames();
				logger.trace("found groups '{}' for item: {}", groups, deviceConfig.getItem().getName());
				if (groups.contains(function.getItem().getName())
						&& groups.contains(place.getItem().getName())) {
					logger.debug("item {} is in requested groups", deviceConfig.getItem().getName());
					list.add(deviceConfig);
				}
			} catch (ItemNotFoundException e) {
				logger.error("cannot find item in registry: {}", deviceConfig.getItem().getName());
				continue;
			}
		}
		
		return list;
	}
}
