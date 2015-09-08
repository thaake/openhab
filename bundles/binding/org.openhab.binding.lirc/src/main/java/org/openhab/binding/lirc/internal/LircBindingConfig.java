package org.openhab.binding.lirc.internal;


import org.openhab.core.binding.BindingConfig;

public class LircBindingConfig implements BindingConfig {

	private String device;
	private String command;

	public LircBindingConfig() {
		super();
	}
	
	public LircBindingConfig(String device, String command) {
		super();
		this.device = device;
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	@Override
	public String toString() {
		return "LircBindingConfig [device=" + device + ", command=" + command
				+ "]";
	}
	
	
}
