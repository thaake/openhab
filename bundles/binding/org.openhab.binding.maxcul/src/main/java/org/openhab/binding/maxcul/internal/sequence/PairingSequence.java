package org.openhab.binding.maxcul.internal.sequence;

import java.util.Date;
import java.util.List;

import org.openhab.binding.maxcul.internal.MaxCulBinding;
import org.openhab.binding.maxcul.internal.MaxCulSender;
import org.openhab.binding.maxcul.internal.message.AckMsg;
import org.openhab.binding.maxcul.internal.message.BaseMsg;
import org.openhab.binding.maxcul.internal.message.MaxCulMsgType;
import org.openhab.binding.maxcul.internal.message.PairPingMsg;
import org.openhab.binding.maxcul.internal.message.PairPongMsg;
import org.openhab.binding.maxcul.internal.message.WakeupMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ping-Sequence
 * Ping -> Pong -> Ack
 * @author Robert
 *
 */
public class PairingSequence extends Sequence {
	private static final Logger LOG = LoggerFactory.getLogger(PairingSequence.class);
	
	public PairingSequence(MaxCulSender sender, List<Sequence> sequences) {
		super(sender, sequences);
	}

	/**
	 * 
	 * @param msg
	 */
	public void addMsg(BaseMsg msg) {
		this.msgList.add(msg);
		
		this.setLastMsg(new Date());
		
		BaseMsg lastMsg = this.msgList.get(this.msgList.size() - 1);
		if (lastMsg instanceof PairPingMsg && this.msgList.size() == 1) {
			BaseMsg pairPongMsg = new PairPongMsg(lastMsg.msgCount, (byte) 0x00, (byte) 0x00, MaxCulBinding.MAXCUL_ADDRESS, lastMsg.srcAddrStr);
			this.sender.send(pairPongMsg);
			this.msgList.add(pairPongMsg);
			this.nextExpMsg = MaxCulMsgType.ACK;
//		} else if (lastMsg instanceof AckMsg && this.msgList.size() == 3) {
//			BaseMsg wakeupMsg = new WakeupMsg(lastMsg.msgCount, (byte) 0x00, (byte) 0x00, MaxCulBinding.MAXCUL_ADDRESS, lastMsg.srcAddrStr);
//			this.sender.send(wakeupMsg);
//			this.msgList.add(wakeupMsg);
//			this.nextExpMsg = MaxCulMsgType.ACK;
		} else if (lastMsg instanceof AckMsg && this.msgList.size() == 3) {
			super.notifyForFinish();
		}
	}

	@Override
	public void run() {
		LOG.info("starting pairing sequence");
		// initiated by device, cannot wait
		while (this.msgList.size() < 3) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }
			// waiting
			
			if (this.timedOut()) {
				break;
			}
		}
		
		LOG.info("pairing sequence ended");
	}

}
