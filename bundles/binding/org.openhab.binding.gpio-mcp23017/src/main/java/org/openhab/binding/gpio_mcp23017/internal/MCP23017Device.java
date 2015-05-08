package org.openhab.binding.gpio_mcp23017.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.gpio_raspberry.device.I2CDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCP23017Device extends I2CDevice<MCP23017Config, MCP23017ItemConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(MCP23017Device.class);
	
	private ExecutorService executors = Executors.newCachedThreadPool();
	private Map<Byte, Runnable> pollingMap = new HashMap<Byte, Runnable>();

	public MCP23017Device(MCP23017Config config) {
		super(config);
	}
	
	public void startPolling(final MCP23017ItemConfig itemConfig, final EventPublisher eventPublisher) {
		if (pollingMap.containsKey(itemConfig.getPort())) {
			// it's already polling
			return;
		}
		
		final byte portMask = (byte) (0x01 << itemConfig.getPort());
		this.setAsInput(portMask, itemConfig.getPort(), itemConfig.getBank());
		
		PollingThread run = new PollingThread(portMask,
				itemConfig.getBank(),
				eventPublisher,
				itemConfig.getItem().getName(),
				itemConfig.getItem().getClass());
		this.pollingMap.put(itemConfig.getPort(), run);
		this.executors.execute(run);
	}
	
	public void stopPolling() {
		this.pollingMap.clear();
		this.executors.shutdownNow();
	}

	@Override
	public State communicate(Command command, final MCP23017ItemConfig itemConfig, State state) {
		if (command instanceof OnOffType) {
			OnOffType onOffType = (OnOffType) command;
			switch (onOffType) {
			case ON:
				change(this.getConfig().getAddress(), itemConfig.getPort(),
						itemConfig.getBank(), true);
				break;
			case OFF:
				change(this.getConfig().getAddress(), itemConfig.getPort(),
						itemConfig.getBank(), false);
				break;
			default:
				throw new IllegalStateException("not implemented state: "
						+ onOffType);
			}
			return null;
		} else if (command instanceof OpenClosedType) {
			OpenClosedType openClosedType = (OpenClosedType) command;
			switch (openClosedType) {
			case OPEN:
				change(this.getConfig().getAddress(), itemConfig.getPort(),
						itemConfig.getBank(), true);
				break;
			case CLOSED:
				change(this.getConfig().getAddress(), itemConfig.getPort(),
						itemConfig.getBank(), false);
				break;
			default:
				throw new IllegalStateException("not implemented state: "
						+ openClosedType);
			}
			return null;
		} else {
			throw new IllegalStateException("not implemented command-type: "
					+ command);
		}
	}
	
	private void change(byte address, byte port, char bank, boolean on) {
		
		byte portAddress = (byte) (0x01 << port);
		byte registerSwitch = -1;
		byte registerDir = -1;
		if (bank == 'a') {
			registerSwitch = 0x12;
			registerDir = 0x00;
		} else if (bank == 'b') {
			registerSwitch = 0x13;
			registerDir = 0x01;
		} else {
			throw new IllegalStateException("unknown bank: " + bank);
		}

		LOG.debug(
				"switch address={}, port={}, registerDir={}, registerPort={}",
				new Object[] { Integer.toHexString(address),
						Integer.toHexString(portAddress),
						Integer.toHexString(registerDir),
						Integer.toHexString(registerSwitch) });

		super.open("/dev/i2c-1");
		try {
			int oldValue = super.read(registerDir);
			LOG.debug("reading dir register: "
					+ Integer.toHexString(oldValue & 0xFF));
			byte newValue = (byte) (oldValue & 0xFF & ~portAddress);
			LOG.debug("setting dir register: "
					+ Integer.toHexString(newValue));
			boolean res = super.write(registerDir, newValue);
			if (!res) {
				throw new IllegalStateException(
						"cannot set port as output, error='" + res + "'");
			}

			oldValue = super.read(registerSwitch);
			LOG.debug("reading port register: "
					+ Integer.toHexString(oldValue));
			if (on) {
				newValue = (byte) (oldValue | portAddress);
			} else {
				newValue = (byte) (oldValue & ~portAddress);
			}
			LOG.debug("setting port register: "
					+ Integer.toHexString(newValue));
			res = super.write(registerSwitch, newValue);
			if (!res) {
				throw new IllegalStateException(
						"cannot read value on port, error='" + res + "'");
			}
		} finally {
			super.close();
		}
	}
	
	private static byte getRegisterSwitch(char bank) {
		if (bank == 'a') {
			return 0x12;
		} else if (bank == 'b') {
			return 0x13;
		} else {
			throw new IllegalStateException("unknown bank: " + bank);
		}
	}
	
	private void setAsInput(byte portMask, byte port, char bank) {
		byte registerDir = -1;
		if (bank == 'a') {
			registerDir = 0x00;
		} else if (bank == 'b') {
			registerDir = 0x01;
		} else {
			throw new IllegalStateException("unknown bank: " + bank);
		}

		LOG.debug("set port '" + port + "' as input (mask="
				+ Integer.toHexString(portMask) + ")");

		super.open("/dev/i2c-1");
		try {
			int oldValue = super.read(registerDir);
			LOG.trace("old mask: " + Integer.toHexString(oldValue & 0xFF));
			byte newValue = (byte) ((oldValue & 0xFF) | portMask);
			LOG.trace("new mask: " + Integer.toHexString(newValue));
			boolean res = super.write(registerDir, newValue);
			if (!res) {
				throw new IllegalStateException(
						"cannot set port as input, error='" + res + "'");
			}
		} finally {
			super.close();
		}
	}

	private void read(byte address, byte portMask, char bank,
			EventPublisher eventPublisher, String itemName,
			Class<? extends Item> itemType) {
		
	}

	private static void updateItem(EventPublisher eventPublisher,
			String itemName, boolean on, Class<? extends Item> itemType) {
		if (itemType.equals(SwitchItem.class)) {
			if (on) {
				eventPublisher.postUpdate(itemName, OnOffType.ON);
			} else {
				eventPublisher.postUpdate(itemName, OnOffType.OFF);
			}
		} else if (itemType.equals(ContactItem.class)) {
			if (on) {
				eventPublisher.postUpdate(itemName, OpenClosedType.CLOSED);
			} else {
				eventPublisher.postUpdate(itemName, OpenClosedType.OPEN);
			}
		} else {
			throw new IllegalStateException("invalid command type: " + itemType);
		}

	}

	class PollingThread extends Thread {
		private final byte portMask;
		private final char bank;
		private final EventPublisher eventPublisher;
		private final String itemName;
		private final Class<? extends Item> itemType;
		
		public PollingThread(byte portMask, char bank, EventPublisher eventPublisher,
				String itemName, Class<? extends Item> itemType) {
			super();
			this.portMask = portMask;
			this.bank = bank;
			this.eventPublisher = eventPublisher;
			this.itemName = itemName;
			this.itemType = itemType;
			LOG.debug("new polling thread created for item: {}", itemName);
		}

		@Override
		public void run() {
			byte registerSwitch = getRegisterSwitch(bank);

			boolean lastState = false;

			while (!isInterrupted()) {
				MCP23017Device.this.open("/dev/i2c-1");
				int read = -1;
				try {
					read = (MCP23017Device.this.read((byte) registerSwitch) & 0xFF);
					LOG.trace("read value: " + Integer.toHexString(read & 0xFF));
				} finally {
					MCP23017Device.this.close();
				}

				byte result = (byte) ((read & 0xFF) & portMask);
				LOG.trace("result: " + Integer.toHexString(result));
				boolean newState = result == portMask;
				if (lastState != newState) {
					if (newState) {
						updateItem(eventPublisher, itemName, true, itemType);
					} else {
						updateItem(eventPublisher, itemName, false, itemType);
					}
					lastState = newState;
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// its ok
				}
			}
		}
		
	}

}
