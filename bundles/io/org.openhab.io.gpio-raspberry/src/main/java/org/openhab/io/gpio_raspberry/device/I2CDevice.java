package org.openhab.io.gpio_raspberry.device;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.openhab.io.gpio_raspberry.item.GpioI2CItemConfig;

import com.pi4j.jni.I2C;


public abstract class I2CDevice<DC extends I2CConfig, IC extends GpioI2CItemConfig> extends Device<DC, IC> {
	protected static final ReentrantLock LOCK = new ReentrantLock(true);
	protected static final int TIMEOUT = 10;
	protected static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
	
	private int handle;
	
	public I2CDevice() {
		super();
	}

	public I2CDevice(DC config) {
		super(config);
	}

	public boolean write(byte register, byte value) {
		int res = I2C.i2cWriteByte(this.handle, this.config.getAddress(), register, value);
		if (res < 0) {
			return false;
		}
		return true;
	}
	
	public boolean write(byte value) {
		int res = I2C.i2cWriteByteDirect(this.handle, this.config.getAddress(), value);
		if (res < 0) {
			return false;
		}
		return true;
	}
	
	protected boolean open(String bus) {
		try {
			if (!LOCK.tryLock(TIMEOUT, TIMEOUT_UNIT)) {
				return false;
			}
		} catch (InterruptedException e) {
			return false;
		}
		this.handle = I2C.i2cOpen(bus);
		return true;
	}
	
	protected void close() {
		I2C.i2cClose(handle);
		if (LOCK.isHeldByCurrentThread()) {
			LOCK.unlock();
		}
	}

	public byte[] readAll() {
		int data = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((data = (I2C.i2cReadByteDirect(this.handle,
				this.config.getAddress()))) != 0) {
			baos.write(data);
		}
		
		try {
			baos.flush();
			baos.close();
		} catch (IOException e) {
			throw new IllegalStateException("cannot close stream", e);
		}
		return baos.toByteArray();
	}

	public int read(byte register) {
		return I2C.i2cReadByte(this.handle, this.config.getAddress(), register);
	}
}
