package org.openhab.binding.gpio_pcf8591.internal;

import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.openhab.io.gpio_raspberry.device.I2CDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCF8591Device extends I2CDevice<PCF8591Config, PCF8591ItemConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(PCF8591Device.class);

	public PCF8591Device(PCF8591Config config) {
		super(config);
	}

	@Override
	public State communicate(Command command, PCF8591ItemConfig itemConfig, State state) {
		LOG.debug("received command: {}", command);
		
		if (itemConfig.isIn()) {
			super.open("/dev/i2c-1");
			// read 2x, the first returns an incorrect value
			int read = super.read((byte) (0x40 + itemConfig.getPort()));
			read = super.read((byte) (0x40 + itemConfig.getPort()));
			if (read < 0) {
				LOG.warn("error reading value: {}", read);
				return org.openhab.core.types.UnDefType.UNDEF;
			}
			read = read & 0xFF;
			int value = (int) Math.rint(read * 100f / 255f);
			super.close();
			LOG.debug("reading value: {}, converted to: {}", read, value);
			
			State stateNew = TypeParser.parseState(itemConfig.getItem().getAcceptedDataTypes(), value + "");
			
			return stateNew;
		} else {
			int value = 0;
			if (state instanceof org.openhab.core.types.UnDefType) {
				state = new PercentType(0);
			}
			
			if (command instanceof PercentType) {
				value = (((PercentType) command).intValue() * 255 / 100);
			} else if (command instanceof OnOffType) {
				if (((OnOffType) command) == OnOffType.ON) {
					value = 255;
				} else if (((OnOffType) command) == OnOffType.OFF) {
					value = 0;
				} else {
					LOG.error("unhandled type: " + command);
				}
			} else if (command instanceof IncreaseDecreaseType) {
				value = Integer.parseInt(state.format("%.0f"));
				switch (((IncreaseDecreaseType) command)) {
					case INCREASE:
						value += 5;
						break;
					case DECREASE:
						value -= 5;
						break;
					default:
						LOG.error("unhandled type: " + command);
						return null;
				}
			} else {
				LOG.warn("invalid command: " + command);
				return null;
			}
			if (value < 0 || value > 256) {
				LOG.debug("value must not be set, value out of range '{}'", value);
				return null;
			}
			LOG.debug("setting value: " + value);
			
			super.open("/dev/i2c-1");
			super.write((byte) 0x40, (byte) value);
			super.close();
			
			State stateNew = TypeParser.parseState(itemConfig.getItem().getAcceptedDataTypes(), value + "");
			
			return stateNew;
		}
	}

}
