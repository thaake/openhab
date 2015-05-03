package org.openhab.io.gpio_raspberry.device;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.openhab.io.gpio_raspberry.item.GpioSPIItemConfig;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;


public abstract class SPIDevice<DC extends SPIConfig, IC extends GpioSPIItemConfig> extends Device<DC, IC> {
	protected static final ReentrantLock LOCK = new ReentrantLock(true);
	protected static final int TIMEOUT = 10;
	protected static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
	
	protected final GpioPinDigitalOutput pinSclk;
	protected final GpioPinDigitalOutput pinMosi;
	protected final GpioPinDigitalInput pinMiso;
	protected final GpioPinDigitalOutput pinCs;

	public SPIDevice() {
		final GpioController gpio = GpioFactory.getInstance();
        this.pinSclk = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "SCLK", PinState.LOW);
        this.pinMosi = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "MOSI", PinState.LOW);
        this.pinMiso = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, "MISO");
        this.pinCs = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "CS", PinState.LOW);
	}
	
	
	
	public SPIDevice(DC config) {
		super(config);
		final GpioController gpio = GpioFactory.getInstance();
        this.pinSclk = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "SCLK", PinState.LOW);
        this.pinMosi = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "MOSI", PinState.LOW);
        this.pinMiso = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, "MISO");
        this.pinCs = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "CS", PinState.LOW);
	}



	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof SPIDevice)) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public void cs(boolean state) {
		this.pinCs.setState(state);
	}

	public void sclk(boolean state) {
		this.pinSclk.setState(state);
	}

	public void mosi(boolean state) {
		this.pinMosi.setState(state);
	}
	
	public boolean isMiso() {
		return this.pinMiso.isHigh();
	}

	protected boolean open() {
		try {
			if (!LOCK.tryLock(TIMEOUT, TIMEOUT_UNIT)) {
				return false;
			}
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	protected void close() {
		if (LOCK.isHeldByCurrentThread()) {
			LOCK.unlock();
		}
	}
}
