package org.openhab.io.gpio_raspberry.device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.gpio_raspberry.item.GpioIOItemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class IODevice extends Device<IOConfig, GpioIOItemConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(IODevice.class);
	
	private Map<Byte, GpioPinDigitalOutput> pinOutputMap = new HashMap<Byte, GpioPinDigitalOutput>();
	private Map<Byte, GpioPinDigitalInput> pinInputMap = new HashMap<Byte, GpioPinDigitalInput>();
	
	public IODevice() {
		super();
	}
	
	public IODevice(IOConfig config) {
		super(config);
	}
	
	public void startPolling(final GpioIOItemConfig itemConfig, final EventPublisher eventPublisher) {
		GpioController controller = GpioFactory.getInstance();
		GpioPinDigitalInput pin = null;
		if (this.pinInputMap.containsKey(itemConfig.getPort())) {
			pin = pinInputMap.get(itemConfig.getPort());
		} else {
			final Pin raspiPin = this.getRaspiPin(itemConfig.getPort());
			LOG.debug("resolved to raspi-pin: " + raspiPin);
			PinPullResistance pinPullResistance = PinPullResistance.PULL_DOWN;
			if (itemConfig.isActiveLow()) {
				pinPullResistance = PinPullResistance.PULL_UP;
			}
			pin = controller.provisionDigitalInputPin(raspiPin, pinPullResistance);
			pinInputMap.put(itemConfig.getPort(), pin);
		}
		
		if (pin.getListeners().size() > 0) {
			return;
		}
		
		LOG.debug("adding listener");
		pin.addListener(new GpioPinListenerDigital() {
			private long timeLastAction = 0;
			
			public void handleGpioPinDigitalStateChangeEvent(
					GpioPinDigitalStateChangeEvent event) {
				LOG.trace("pin {} change to {}", event.getPin().getPin().getAddress(), event.getState().isHigh());
				
				boolean on = event.getState().isHigh();
				if (itemConfig.isActiveLow()) {
					on = !on;
				}
				
				if (on && (System.currentTimeMillis() - timeLastAction) < itemConfig.getDebounce()) {
					LOG.trace("pin change is inside debounce interval, ignoring");
					return;
				}
				
				if (on) {
					eventPublisher.postUpdate(itemConfig.getItemName(), OnOffType.ON);
				} else {
					eventPublisher.postUpdate(itemConfig.getItemName(), OnOffType.OFF);
				}
				
				timeLastAction = System.currentTimeMillis();
			}
			
		});
	}
	
	public void stopPolling() {
		for (GpioPinDigitalInput input : this.pinInputMap.values()) {
			input.removeAllListeners();
			input.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
		}
	}

	@Override
	public State communicate(Command command, GpioIOItemConfig itemConfig, State state) {
		LOG.debug("communicate with gpio device");
		
		GpioController controller = GpioFactory.getInstance();
		LOG.debug("GPIO controller created");
		
		LOG.debug("setting output pin...");
		GpioPinDigitalOutput pin = null;
		if (this.pinOutputMap.containsKey(itemConfig.getPort())) {
			pin = pinOutputMap.get(itemConfig.getPort());
		} else {
			final Pin raspiPin = this.getRaspiPin(itemConfig.getPort());
			LOG.debug("resolved to raspi-pin: " + raspiPin);
			pin = controller.provisionDigitalOutputPin(raspiPin);
			pinOutputMap.put(itemConfig.getPort(), pin);
		}
		
		
		try {
			if (command instanceof OnOffType) {
				boolean on = false;
				if (((OnOffType) command) == OnOffType.ON) {
					on = true;
				} else if (((OnOffType) command) == OnOffType.OFF) {
					on = false;
				} else {
					throw new IllegalStateException("not implemented");
				}
				if (itemConfig.isActiveLow()) {
					on = !on;
				}
				LOG.debug("set pin '" + itemConfig + "' to: " + on);
				pin.setState(on);
			} else {
				throw new IllegalStateException("unsupported command: " + command);
			}
		} finally {
			pin.unexport();
		}
		return null;
	}
	
	
	private Pin getRaspiPin(Byte port) {
		if (port == null) {
			throw new IllegalStateException("port is not set");
		}
		LOG.debug("resolving port '" + port + "' to raspi pin");
		switch (port) {
		case 0:
			return RaspiPin.GPIO_00;
		case 1:
			return RaspiPin.GPIO_01;
		case 2:
			return RaspiPin.GPIO_02;
		case 3:
			return RaspiPin.GPIO_03;
		case 4:
			return RaspiPin.GPIO_04;
		case 5:
			return RaspiPin.GPIO_05;
		case 6:
			return RaspiPin.GPIO_06;
		case 7:
			return RaspiPin.GPIO_07;
		case 8:
			return RaspiPin.GPIO_08;
		case 9:
			return RaspiPin.GPIO_09;
		case 10:
			return RaspiPin.GPIO_10;
		case 11:
			return RaspiPin.GPIO_11;
		case 12:
			return RaspiPin.GPIO_12;
		case 13:
			return RaspiPin.GPIO_13;
		case 14:
			return RaspiPin.GPIO_14;
		case 15:
			return RaspiPin.GPIO_15;
		case 16:
			return RaspiPin.GPIO_16;
		case 17:
			return RaspiPin.GPIO_17;
		case 18:
			return RaspiPin.GPIO_18;
		case 19:
			return RaspiPin.GPIO_19;
		case 20:
			return RaspiPin.GPIO_20;
		default:
			throw new IllegalStateException("not a valid port: " + port);
		}
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof IODevice)) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
