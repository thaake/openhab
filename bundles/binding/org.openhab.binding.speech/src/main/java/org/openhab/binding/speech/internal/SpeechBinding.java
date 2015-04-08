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
import java.util.Dictionary;
import java.util.List;

import org.openhab.binding.speech.SpeechBindingProvider;

import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.StringType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 * 
 * @author Robert Delbrück
 * @since 1.7.0
 */
public class SpeechBinding extends AbstractActiveBinding<SpeechBindingProvider>
		implements ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(SpeechBinding.class);

	private long minimumRefresh = 1000;

	private ItemRegistry itemRegistry;
	private TransformationService transformationService;

	public SpeechBinding() {
	}

	public void setItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = itemRegistry;
	}

	public void unsetItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = null;
	}

	public void activate() {
		transformationService = TransformationHelper.getTransformationService(SpeechActivator.getContext(), "MAP");
		System.out.println("bla");
		new Thread() {
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				internalReceiveCommand("VoiceCommand", new StringType("Licht im Schlafzimmer einschalten"));
			}
		}.start();
	}

	public void deactivate() {
		// deallocate resources here that are no longer needed and
		// should be reset when activating this binding again
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		if (itemName.equals("VoiceCommand")) {
			StringType type = ((StringType) command);
			List<String> switchedItems = this.handleSpeech(type.format("%s").toLowerCase());
			try {
				Item item = itemRegistry.getItem("VoiceResponse");
				if (switchedItems == null) {
					this.eventPublisher.postCommand(item.getName(), new StringType(""));
				} else {
					StringBuilder sb = new StringBuilder();
					for (String str : switchedItems) {
						if (sb.length() > 0) {
							sb.append("##");
						}
						sb.append(str);
					}
					this.eventPublisher.postCommand(item.getName(), new StringType(sb.toString()));
				}
			} catch (ItemNotFoundException e) {
				logger.warn("cannot find 'VoiceResponse'");
			}
		}
	}
	
	private String resolveCommand(String word) {
		try {
			String res = this.transformationService.transform("speech.map", word);
			if (res.isEmpty()) {
				return null;
			}
			return res;
		} catch (TransformationException e) {
			logger.error("cannot resolve word", e);
			return null;
		}
	}
	
	private String findCommandInMap(String voice) {
		String[] words = voice.split("\\s");
		for (String word : words) {
			String tmp = this.resolveCommand(word);
			if (tmp != null) {
				return tmp;
			}
		}
		return null;
	}
	
	private String findCommand(String voice, SpeechCommandItemConfig device) {
		for (String command : device.getCommands().keySet()) {
			if (voice.contains(command)) {
				return device.getCommands().get(command);
			}
		}
		return null;
	}
	
	private List<String> handleSpeech(String voice) {
		SpeechBindingProvider provider = null;
		for (SpeechBindingProvider speechBindingProvider : this.providers) {
			provider = speechBindingProvider;
		}
		if (provider == null) {
			logger.error("cannot find provider");
			throw new IllegalStateException("cannot find provider");
		}
		
		logger.trace("looking for config in provider '{}' with text: {}", provider, voice);
		

		List<String> switchedItems = this.handleSpeechForItems(provider, voice);
		if (switchedItems != null) {
			return switchedItems;
		}
		switchedItems = this.handleSpeechForGroups(provider, voice);
		if (switchedItems != null) {
			return switchedItems;
		}
		
		logger.warn("cannot find any items to switch");
		return null;
	}
	
	private void changeDevice(Item item, String command) {
		Command com = TypeParser.parseCommand(item.getAcceptedCommandTypes(), command);
		if (com == null) {
			logger.warn("cannot convert '{}' to a accepted command", command);
			return;
		}
		eventPublisher.sendCommand(item.getName(), com);
		logger.info("device '{}' updated to '{}'", item.getName(), com);
	}
	
	private List<String> handleSpeechForItems(SpeechBindingProvider provider, String voice) {
		logger.debug("looking in items...");
		List<SpeechCommandItemConfig> itemConfigs = new ArrayList<SpeechCommandItemConfig>();
		boolean someHasCommands = false;
		for (SpeechCommandItemConfig speechItemConfig : provider.getAllItemCommandConfigs()) {
			if (matches(voice, speechItemConfig)
					&& speechItemConfig.getType() == Type.DEVICE) {
				itemConfigs.add(speechItemConfig);
				if (speechItemConfig.getCommands().size() > 0) {
					someHasCommands = true;
				}
			}
		}
		
		if (itemConfigs.size() == 0) {
			logger.debug("found no items");
			return null;
		}
		
		if (someHasCommands) {
			List<String> switchedItems = new ArrayList<String>();
			for (SpeechCommandItemConfig config : itemConfigs) {
				if (config.getCommands().size() > 0) {
					// handle all with commands
					String foundCommand = this.findCommand(voice, config);
					if (foundCommand != null) {
						changeDevice(config.getItem(), foundCommand);
						switchedItems.add(config.getPrimaryName() + "#" + foundCommand);
					}
				}
			}
			return switchedItems;
		} else {
			List<String> switchedItems = new ArrayList<String>();
			for (SpeechCommandItemConfig config : itemConfigs) {
				String foundCommand = findCommandInMap(voice);
				if (foundCommand != null) {
					changeDevice(config.getItem(), foundCommand);
					switchedItems.add(config.getPrimaryName() + "#" + foundCommand);
				}
			}
			return switchedItems;
		}
	}

	public List<String> handleSpeechForGroups(SpeechBindingProvider provider, String voice) {
		logger.debug("looking in groups...");
		List<SpeechCommandItemConfig> functionGroupConfigs = new ArrayList<SpeechCommandItemConfig>();
		List<SpeechCommandItemConfig> placeGroupConfigs = new ArrayList<SpeechCommandItemConfig>();
		for (SpeechCommandItemConfig speechItemConfig : provider.getAllItemCommandConfigs()) {
			if (matches(voice, speechItemConfig)) {
				if (speechItemConfig.isGroup()) {
					if (speechItemConfig.getType() == Type.FUNCTION) {
						functionGroupConfigs.add(speechItemConfig);
						logger.debug("found function item: {}", speechItemConfig.getItem().getName());
					} else if (speechItemConfig.getType() == Type.PLACE) {
						placeGroupConfigs.add(speechItemConfig);
						logger.debug("found place item: {}", speechItemConfig.getItem().getName());
					}
				}
			}
		}
		
		if (functionGroupConfigs.size() == 0) {
			logger.debug("cannot found a function group");
			return null;
		}
		
		if (placeGroupConfigs.size() == 0) {
			logger.debug("cannot found a place group");
			return null;
		}
		
		List<SpeechCommandItemConfig> deviceConfig = new ArrayList<SpeechCommandItemConfig>();
		for (SpeechCommandItemConfig functionConfig : functionGroupConfigs) {
			for (SpeechCommandItemConfig placeConfig : placeGroupConfigs) {
				deviceConfig.addAll(provider.getAllItemCommandConfigsForGroups(functionConfig, placeConfig));		
			}
		}
		
		List<String> switchedItems = new ArrayList<String>();
		for (SpeechCommandItemConfig config : deviceConfig) {
			String foundCommand = findCommandInMap(voice);
			if (foundCommand != null) {
				changeDevice(config.getItem(), foundCommand);
				switchedItems.add(config.getPrimaryName() + "#" + foundCommand);
			}
		}
		
		return switchedItems;
	}
	
	private boolean matches(String text, SpeechCommandItemConfig config) {
		if (!config.getPrimaryName().isEmpty() && text.contains(config.getPrimaryName())) {
//			logger.trace("'{}' contains '{}'", text, config.getPrimaryName());
			return true;
		}
		for (String alternative : config.getAlternativeNames()) {
			if (!alternative.isEmpty() && text.contains(alternative)) {
//				logger.trace("'{}' contains '{}'", text, alternative);
				return true;
			}
		}
		return false;
	}

	/**
	 * @{inheritDoc
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
		setProperlyConfigured(true);
	}

	@Override
	protected void execute() {
		
	}

	@Override
	protected long getRefreshInterval() {
		return minimumRefresh;
	}

	@Override
	protected String getName() {
		return "Speech Service";
	}
}
