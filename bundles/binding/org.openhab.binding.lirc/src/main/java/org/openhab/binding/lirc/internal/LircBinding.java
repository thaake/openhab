/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.lirc.internal;

import java.util.Dictionary;
import java.util.Timer;
import java.util.TimerTask;

import org.openhab.binding.lirc.LircBindingProvider;
import org.openhab.binding.lirc.internal.LircTransceiver.LircEventListener;
import org.openhab.binding.lirc.internal.LircTransceiver.ReceivedData;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Main binding class for the Lirc binding.
 * 
 * @author Robert Delbrück
 * @since 1.6.1
 *
 */
public class LircBinding extends AbstractBinding<LircBindingProvider> implements ManagedService, LircEventListener {

	private static final Logger logger = LoggerFactory.getLogger(LircBinding.class);
	
	private static final String PROP_HOST = "host";
	private static final String PROP_PORT = "port";

	private LircTransceiver transceiver;
	
	private String host;
	private Integer port;
	
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties != null) {
			if (properties.get(PROP_HOST) == null) {
				throw new ConfigurationException(PROP_HOST, "required, but unset");
			}
			if (properties.get(PROP_PORT) == null) {
				throw new ConfigurationException(PROP_PORT, "required, but unset");
			}
			
			this.host = properties.get(PROP_HOST) + "";
			this.port = Integer.parseInt(properties.get(PROP_PORT) + "");
			logger.debug("host '{}' and port '{}' successfully read", host, port);
		}
	}
	
	

	@Override
	public void activate() {
		super.activate();
		
		Timer timer = new Timer("reconnect timer", true);
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if (host == null || port == null) {
					logger.info("configuration is currently not loaded, waiting with connection...");
					return;
				}
				logger.trace("checking if tranceiver is running...");
				if (transceiver == null) {
					logger.trace("creating new tranceiver...");
					transceiver = new LircTransceiver(host, port);
				}
				if (transceiver != null && !transceiver.isConnected()) {
					logger.trace("tranceiver not connected, connecting...");
					if (transceiver.connect()) {
						transceiver.addListener(LircBinding.this);
						transceiver.startListener();
						transceiver.startSender();
					}
				}
			}
		}, 10000, 30000);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		
		this.transceiver.removeListener(this);
		this.transceiver.disconnect();
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		LircBindingProvider provider = null;
		for (LircBindingProvider providerInList : providers) {
			provider = providerInList;
		}
		
		if (provider == null) {
			logger.error("no provider found");
			return;
		}
		
		if (command != OnOffType.ON) {
			logger.debug("just on commands are handled, received: {}", command);
			return;
		}
		
		if (transceiver == null) {
			logger.error("transceiver is null!");
			return;
		}
		
		this.transceiver.send(provider.getConfig(itemName));
		this.eventPublisher.postCommand(itemName, OnOffType.OFF);
	}

	@Override
	public void received(ReceivedData data) {
		LircBindingProvider provider = null;
		for (LircBindingProvider providerInList : providers) {
			provider = providerInList;
		}
		
		if (provider == null) {
			logger.error("no provider found");
			return;
		}
		
		logger.debug("switching items for {}:{}", data.getDevice(), data.getCommand());
		for (String itemName : provider.getItemNames()) {
			// 000000000000213c 00 VOLUME_DOWN av-receiver
			LircBindingConfig config = provider.getConfig(itemName);
			
			if (!config.getDevice().equals(data.getDevice())) {
				logger.trace("cannot find device '{}'", data.getDevice());
				continue;
			}
			
			if (!config.getCommand().equals(data.getCommand())) {
				logger.trace("cannot find command '{}' for device '{}'", data.getCommand(), data.getDevice());
				continue;
			}
			
			logger.debug("switch item: {}", itemName);
			eventPublisher.postUpdate(itemName, OnOffType.ON);
			eventPublisher.postUpdate(itemName, OnOffType.OFF);
		}
		
	}

}
