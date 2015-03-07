package org.openhab.binding.maxcul.internal.message;

import junit.framework.Assert;

import org.junit.Test;
import org.openhab.binding.maxcul.internal.messages.ShutterContactMsg;

public class TestShutterContact {
	@Test
	public void testConstructor() {
		ShutterContactMsg msg = new ShutterContactMsg("Z0B17063000700300AC4D0010F7");
		Assert.assertTrue(msg.getBatteryLow());
	}
}
