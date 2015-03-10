/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.maxcul.internal;

/**
 * Define binding features
 * 
 * @author Paul Hampson (cyclingengineer)
 * @since 1.6.0
 */
public enum MaxCulFeature {
	SETTEMPERATURE, ACTTEMPERATURE, BATTERY, MODE, SWITCH, VALVEPOS, RESET, CONTACT, UNKNOWN;
	
	public static MaxCulFeature parse(String val) {
		for (MaxCulFeature feature : values()) {
			if (feature.name().equalsIgnoreCase(val)) {
				return feature;
			}
		}
		return null;
	}
}
