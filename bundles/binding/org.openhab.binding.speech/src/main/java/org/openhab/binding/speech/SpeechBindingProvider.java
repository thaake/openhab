/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.speech;

import java.util.List;

import org.openhab.binding.speech.internal.SpeechCommandItemConfig;
import org.openhab.binding.speech.internal.SpeechItemConfig;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * @author Robert Delbrück
 * @since 1.7.0
 */
public interface SpeechBindingProvider extends BindingProvider {

	boolean isItemConfigured(String itemName);
	
	SpeechItemConfig getItemConfig(String itemName);
	List<SpeechCommandItemConfig> getAllItemCommandConfigs();

	List<SpeechCommandItemConfig> getItemConfigs(List<Item> itemList);

}
