package org.openhab.binding.maxcul.internal;

import org.openhab.binding.maxcul.internal.message.BaseMsg;

public interface MaxCulSender {
	public void send(BaseMsg msg);
	public byte getMessageNumber();
}
