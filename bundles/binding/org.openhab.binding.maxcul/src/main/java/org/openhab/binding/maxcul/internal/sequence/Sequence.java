package org.openhab.binding.maxcul.internal.sequence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.openhab.binding.maxcul.internal.MaxCulBinding;
import org.openhab.binding.maxcul.internal.MaxCulSender;
import org.openhab.binding.maxcul.internal.message.BaseMsg;
import org.openhab.binding.maxcul.internal.message.MaxCulMsgType;
import org.openhab.binding.maxcul.internal.message.PairPingMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Delbrück
 *
 */
public abstract class Sequence implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(Sequence.class);
	protected List<BaseMsg> msgList = new ArrayList<BaseMsg>();
	protected MaxCulSender sender;
	protected MaxCulMsgType nextExpMsg;
	private List<SequenceListener> listenerList = new ArrayList<SequenceListener>();
	protected Date lastMsg;
	protected List<Sequence> sequences;
	
	public Sequence(MaxCulSender sender, List<Sequence> sequences) {
		super();
		this.sender = sender;
		this.sequences = sequences;
		this.lastMsg = new Date();
	}
	
	protected boolean amIActive() {
		return this.sequences.get(0) == this;
	}

	/**
	 * 
	 * @param msg
	 */
	public abstract void addMsg(BaseMsg msg);

	public MaxCulMsgType getNextExpMsg() {
		return nextExpMsg;
	}

	public void setNextExpMsg(MaxCulMsgType nextExpMsg) {
		this.nextExpMsg = nextExpMsg;
	}

	public String getSourceDeviceAddress() {
		if (this.msgList.size() == 0) {
			LOG.error("cannot start sequence, no message inside");
			return "";
		}
		return this.msgList.get(0).srcAddrStr;
	}
	
	public boolean doesMsgMatch(BaseMsg msg) {
		
		return this.getNextExpMsg().equals(msg.msgType)
				&& this.getSourceDeviceAddress().equals(msg.srcAddrStr);
	}

	public String getSourceSerialNumber() {
		if (this.msgList.size() == 0) {
			LOG.error("cannot start sequence, no message inside");
			return "";
		}
		return ((PairPingMsg) this.msgList.get(0)).serial;
	}

	public void addSequenceListener(
			SequenceListener l) {
		this.listenerList.add(l);
	}
	
	public void removeSequenceListener(
			SequenceListener l) {
		this.listenerList.remove(l);
	}
	
	protected void notifyForFinish() {
		for (SequenceListener l : this.listenerList) {
			l.finished();
		}
	}
	
	protected void notifyForTimeout() {
		for (SequenceListener l : this.listenerList) {
			l.timedOut();
		}
	}
	
	public Date getLastMsg() {
		return lastMsg;
	}

	protected void setLastMsg(Date lastMsg) {
		this.lastMsg = lastMsg;
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				notifyForTimeout();
			}
		}, new Date(lastMsg.getTime() + 10000));
	}
	
	protected boolean timedOut() {
		if (new Date().getTime() - this.lastMsg.getTime() > MaxCulBinding.MSG_TIMEOUT) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getSourceSerialNumber() == null) ? 0 : getSourceSerialNumber().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sequence other = (Sequence) obj;
		if (!getSourceSerialNumber().equals(other.getSourceSerialNumber()))
			return false;
		return true;
	}
	
	
}
