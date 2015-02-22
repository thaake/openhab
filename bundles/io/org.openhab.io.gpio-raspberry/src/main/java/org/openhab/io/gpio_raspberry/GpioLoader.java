package org.openhab.io.gpio_raspberry;

import org.openhab.io.gpio_raspberry.device.I2CConfig;
import org.openhab.io.gpio_raspberry.device.I2CDevice;
import org.openhab.io.gpio_raspberry.device.IOConfig;
import org.openhab.io.gpio_raspberry.device.IODevice;
import org.openhab.io.gpio_raspberry.device.SPIConfig;
import org.openhab.io.gpio_raspberry.device.SPIDevice;
import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;
import org.openhab.io.gpio_raspberry.item.GpioSPIItemConfig;

public interface GpioLoader {

	public abstract <DC extends I2CConfig, IC extends GpioI2CItemConfig> I2CDevice<DC, IC> createI2CDevice(
			DC config, Class<? extends I2CDevice<DC, IC>> deviceType)
			throws GpioException;
	
	public abstract IODevice createIODevice(
			IOConfig config, Class<? extends IODevice> deviceType)
			throws GpioException;

	public abstract <DC extends SPIConfig, IC extends GpioSPIItemConfig> SPIDevice<DC, IC> createSPIDevice(
			DC config,
			Class<? extends SPIDevice<DC, IC>> deviceType)
			throws GpioException;

}