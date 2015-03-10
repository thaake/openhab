package org.openhab.binding.maxcul.dep.internal.messages;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutterContactMsg extends BaseMsg {
	private final static Logger logger = LoggerFactory.getLogger(ShutterContactMsg.class);
	
	private boolean opened;
	private boolean batteryLow;

	public ShutterContactMsg(String rawMsg) {
		super(rawMsg);
		
		logger.debug(this.msgType + " Payload Len -> " + this.payload.length);

		logger.info("payload: " + new HexBinaryAdapter().marshal(payload));
		
		this.opened = extractBitFromByte(payload[0], 1);
		this.batteryLow = extractBitFromByte(payload[0], 7);
	}

	public boolean getOpened() {
		return opened;
	}

	public boolean getBatteryLow() {
		return batteryLow;
	}

}
