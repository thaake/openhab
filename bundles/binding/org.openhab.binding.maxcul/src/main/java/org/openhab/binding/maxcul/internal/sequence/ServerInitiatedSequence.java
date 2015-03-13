package org.openhab.binding.maxcul.internal.sequence;

import java.util.Date;
import java.util.List;

import org.openhab.binding.maxcul.internal.MaxCulBinding;
import org.openhab.binding.maxcul.internal.MaxCulSender;
import org.openhab.binding.maxcul.internal.message.AckMsg;
import org.openhab.binding.maxcul.internal.message.BaseMsg;
import org.openhab.binding.maxcul.internal.message.MaxCulMsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ping-Sequence
 * Ping -> Pong -> Ack
 * @author Robert
 *
 */
public class ServerInitiatedSequence extends Sequence {
	private static final Logger LOG = LoggerFactory.getLogger(ServerInitiatedSequence.class);
	
	public ServerInitiatedSequence(MaxCulSender sender, List<Sequence> sequences) {
		super(sender, sequences);
	}

	/**
	 * 
	 * @param msg
	 * @return true if sequence is finished
	 */
	public void addMsg(BaseMsg msg) {
		this.msgList.add(msg);
		this.setLastMsg(new Date());
	}

	@Override
	public void run() {
		LOG.info("starting server initiated sequence");
		
		BaseMsg lastMsg = this.msgList.get(this.msgList.size() - 1);
		if (lastMsg.srcAddrStr.equals(MaxCulBinding.MAXCUL_ADDRESS)) {
			// must be send first
			this.sender.send(lastMsg);
			this.nextExpMsg = MaxCulMsgType.ACK;
		} else {
			while (true) {
				if (lastMsg instanceof AckMsg) {
					super.notifyForFinish();
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
				// waiting
				
				if (this.timedOut()) {
					break;
				}
			}
		}
		
		LOG.info("server initiated sequence ended");
	}

}
