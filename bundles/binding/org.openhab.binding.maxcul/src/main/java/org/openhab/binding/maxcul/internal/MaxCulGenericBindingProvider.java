package org.openhab.binding.maxcul.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.maxcul.MaxCulBindingProvider;
import org.openhab.binding.maxcul.internal.config.MaxCulDeviceConfig;
import org.openhab.binding.maxcul.internal.config.MaxCulFeatureConfig;
import org.openhab.binding.maxcul.internal.config.MaxCulItemConfig;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * 
 * @author Robert Delbrück
 * 
 * maxcul="device:RadiatorThermostat serialNumber:JE343249 feature:battery"
 * maxcul="feature:listen"
 * maxcul="feature:pair"
 *
 */
public class MaxCulGenericBindingProvider extends
		AbstractGenericBindingProvider implements MaxCulBindingProvider {
	
	private static final String REGEX_DEVICE = "device:([A-Za-z0-9]+)";
	private static final String REGEX_FEATURE = "feature:([A-Za-z0-9]+)";
	private static final String REGEX_SERIAL_NUMBER = "serialNumber:([A-Za-z0-9]+)";

	@Override
	public String getBindingType() {
		return "maxcul";
	}
	
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		
		String device = null;
		String feature = null;
		String serialNumber = null;
		
		String[] split = bindingConfig.split(" ");
		for (String part : split) {
			Matcher mDevice = Pattern.compile(REGEX_DEVICE).matcher(part);
			if (mDevice.matches()) {
				device = mDevice.group(1);
			}
			
			Matcher mFeature = Pattern.compile(REGEX_FEATURE).matcher(part);
			if (mFeature.matches()) {
				feature = mFeature.group(1);
			}
			
			Matcher mSerialNumber = Pattern.compile(REGEX_SERIAL_NUMBER).matcher(part);
			if (mSerialNumber.matches()) {
				serialNumber = mSerialNumber.group(1);
			}
		}
		
		if (device == null && feature == null) {
			throw new BindingConfigParseException("invalid configuration, feature and/or device is missing");
		}
		
		if (device != null && serialNumber != null) {
			if (feature == null) {
				throw new BindingConfigParseException("missing feature");
			}
			
			MaxCulDeviceConfig config = new MaxCulDeviceConfig();
			config.setItem(item);
			config.setDevice(MaxCulDevice.parse(device));
			config.setFeature(MaxCulFeature.parse(feature));
			config.setSerialNumber(serialNumber);
			config.loadStoredConfig();
			
			super.addBindingConfig(item, config);
		} else if (feature != null) {
			MaxCulFeatureConfig config = new MaxCulFeatureConfig();
			config.setItem(item);
			if (feature.equals("listen")) {
				config.setListen(true);
			} else if (feature.equals("pair")) {
				config.setPair(true);
			}
			super.addBindingConfig(item, config);
		}
	}
	
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
	}

	@Override
	public MaxCulDeviceConfig getConfigForSerialNumber(String serial) {
		for (BindingConfig bindingConfig : this.bindingConfigs.values()) {
			if (bindingConfig instanceof MaxCulDeviceConfig) {
				if (((MaxCulDeviceConfig) bindingConfig).getSerialNumber().equals(serial)) {
					return (MaxCulDeviceConfig) bindingConfig;
				}
			}
		}
		return null;
	}



	@Override
	public MaxCulFeatureConfig getFeatureConfig(String itemName) {
		MaxCulItemConfig config = (MaxCulItemConfig) this.bindingConfigs.get(itemName);
		if (config instanceof MaxCulFeatureConfig) {
			return (MaxCulFeatureConfig) config;
		}
		return null;
	}



	@Override
	public MaxCulDeviceConfig getDeviceConfig(String itemName) {
		MaxCulItemConfig config = (MaxCulItemConfig) this.bindingConfigs.get(itemName);
		if (config instanceof MaxCulDeviceConfig) {
			return (MaxCulDeviceConfig) config;
		}
		return null;
	}

}
