package org.openhab.io.gpio_raspberry.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.openhab.core.service.AbstractActiveService;
import org.openhab.io.gpio_raspberry.GpioException;
import org.openhab.io.gpio_raspberry.GpioLoader;
import org.openhab.io.gpio_raspberry.device.I2CConfig;
import org.openhab.io.gpio_raspberry.device.I2CDevice;
import org.openhab.io.gpio_raspberry.device.IOConfig;
import org.openhab.io.gpio_raspberry.device.IODevice;
import org.openhab.io.gpio_raspberry.device.SPIConfig;
import org.openhab.io.gpio_raspberry.device.SPIDevice;
import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;
import org.openhab.io.gpio_raspberry.item.GpioSPIItemConfig;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioLoaderImpl extends AbstractActiveService implements ManagedService, GpioLoader {
	private static final Logger LOG = LoggerFactory.getLogger(GpioLoaderImpl.class);
	
	private Map<String, I2CDevice<I2CConfig, GpioI2CItemConfig>> deviceI2CList = new HashMap<String, I2CDevice<I2CConfig, GpioI2CItemConfig>>();
	private Map<String, SPIDevice<SPIConfig, GpioSPIItemConfig>> deviceSPIList = new HashMap<String, SPIDevice<SPIConfig, GpioSPIItemConfig>>();
	private Map<String, IODevice> deviceIOList = new HashMap<String, IODevice>();

	/* (non-Javadoc)
	 * @see org.openhab.io.gpio_raspberry.GpioLoader#createDevice(DC, java.lang.Class)
	 */
	public <DC extends I2CConfig, IC extends GpioI2CItemConfig> I2CDevice<DC, IC> createI2CDevice(DC config, Class<? extends I2CDevice<DC, IC>> deviceType) throws GpioException {
		if (config instanceof I2CConfig) {
			if (deviceI2CList.containsKey(config.getId())) {
//				I2CDevice i2cDevice = (I2CDevice) deviceI2CList.get(config.getId());
//				LOG.debug("device '{}' is already in map, returning this: {}", config.getId(), i2cDevice);
//				return i2cDevice;
				this.deviceI2CList.remove(config.getId());
			}
			
			try {
				LOG.debug("creating new device for '{}'", config.getId());
				Constructor<?> constructor = deviceType.getConstructor(config.getClass());
				I2CDevice device = (I2CDevice) constructor.newInstance(config);
				this.deviceI2CList.put(config.getId(), device);
				return device;
			} catch (InstantiationException e) {
				throw new GpioException("cannot create device", e);
			} catch (IllegalAccessException e) {
				throw new GpioException("cannot create device", e);
			} catch (IllegalArgumentException e) {
				throw new GpioException("cannot create device", e);
			} catch (InvocationTargetException e) {
				throw new GpioException("cannot create device", e);
			} catch (NoSuchMethodException e) {
				throw new GpioException("cannot create device", e);
			} catch (SecurityException e) {
				throw new GpioException("cannot create device", e);
			}
				
		} else {
			throw new GpioException("unexpected config type");
		}
		
	}
	
	public <DC extends SPIConfig, IC extends GpioSPIItemConfig> SPIDevice<DC, IC> createSPIDevice(DC config, Class<? extends SPIDevice<DC, IC>> deviceType) throws GpioException {
		if (config instanceof SPIConfig) {
			if (deviceI2CList.containsKey(config.getId())) {
				return (SPIDevice<DC, IC>) deviceSPIList.get(config.getId());
			} else {
				try {
					Constructor<?> constructor = deviceType.getConstructor(config.getClass());
					SPIDevice device = (SPIDevice) constructor.newInstance(config);
					this.deviceSPIList.put(config.getId(), device);
					return device;
				} catch (InstantiationException e) {
					throw new GpioException("cannot create device", e);
				} catch (IllegalAccessException e) {
					throw new GpioException("cannot create device", e);
				} catch (IllegalArgumentException e) {
					throw new GpioException("cannot create device", e);
				} catch (InvocationTargetException e) {
					throw new GpioException("cannot create device", e);
				} catch (NoSuchMethodException e) {
					throw new GpioException("cannot create device", e);
				} catch (SecurityException e) {
					throw new GpioException("cannot create device", e);
				}
				
			}
		} else {
			throw new GpioException("unexpected config type");
		}
		
	}

	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties != null) {
			System.out.println(properties);
		}
	}



	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}



	@Override
	protected long getRefreshInterval() {
		return 0;
	}



	@Override
	protected String getName() {
		return "gpio-raspberry";
	}



	public IODevice createIODevice(IOConfig config,
			Class<? extends IODevice> deviceType) throws GpioException {
		if (config instanceof IOConfig) {
			if (deviceIOList.containsKey(config.getId())) {
				return (IODevice) deviceIOList.get(config.getId());
			} else {
				try {
					Constructor<?> constructor = deviceType.getConstructor(config.getClass());
					IODevice device = (IODevice) constructor.newInstance(config);
					this.deviceIOList.put(config.getId(), device);
					return device;
				} catch (InstantiationException e) {
					throw new GpioException("cannot create device", e);
				} catch (IllegalAccessException e) {
					throw new GpioException("cannot create device", e);
				} catch (IllegalArgumentException e) {
					throw new GpioException("cannot create device", e);
				} catch (InvocationTargetException e) {
					throw new GpioException("cannot create device", e);
				} catch (NoSuchMethodException e) {
					throw new GpioException("cannot create device", e);
				} catch (SecurityException e) {
					throw new GpioException("cannot create device", e);
				}
				
			}
		} else {
			throw new GpioException("unexpected config type");
		}
	}

}
