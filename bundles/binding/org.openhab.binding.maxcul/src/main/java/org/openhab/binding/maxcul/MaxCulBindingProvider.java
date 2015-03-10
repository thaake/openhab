/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.maxcul;

import org.openhab.binding.maxcul.internal.config.MaxCulDeviceConfig;
import org.openhab.binding.maxcul.internal.config.MaxCulFeatureConfig;
import org.openhab.core.binding.BindingProvider;

/**
 * @author Paul Hampson (cyclingengineer)
 * @since 1.6.0
 */
public interface MaxCulBindingProvider extends BindingProvider {

	/**
	 * This will return the first config found for a particular serial number
	 * 
	 * @param serial
	 *            Serial number of device
	 * @return First configuration found, null if none are found
	 */
	MaxCulDeviceConfig getConfigForSerialNumber(String serial);

	MaxCulFeatureConfig getFeatureConfig(String itemName);

	MaxCulDeviceConfig getDeviceConfig(String itemName);
}
