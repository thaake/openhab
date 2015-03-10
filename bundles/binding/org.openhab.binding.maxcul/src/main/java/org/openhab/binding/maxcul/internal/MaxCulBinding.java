package org.openhab.binding.maxcul.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openhab.binding.maxcul.MaxCulBindingProvider;
import org.openhab.binding.maxcul.internal.config.MaxCulDeviceConfig;
import org.openhab.binding.maxcul.internal.config.MaxCulFeatureConfig;
import org.openhab.binding.maxcul.internal.message.AddLinkPartnerMsg;
import org.openhab.binding.maxcul.internal.message.BaseMsg;
import org.openhab.binding.maxcul.internal.message.PairPingMsg;
import org.openhab.binding.maxcul.internal.sequence.PairingSequence;
import org.openhab.binding.maxcul.internal.sequence.Sequence;
import org.openhab.binding.maxcul.internal.sequence.SequenceListener;
import org.openhab.binding.maxcul.internal.sequence.ServerInitiatedSequence;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.io.transport.cul.CULCommunicationException;
import org.openhab.io.transport.cul.CULDeviceException;
import org.openhab.io.transport.cul.CULHandler;
import org.openhab.io.transport.cul.CULListener;
import org.openhab.io.transport.cul.CULManager;
import org.openhab.io.transport.cul.CULMode;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * maxcul:address=network:192.168.0.222 
 * maxcul:room:livingroom:group=1
 * maxcul:room:livingroom:devices=JEQ0275817,JEQ0084628
 * 
 * @author Robert
 *
 */
public class MaxCulBinding extends AbstractBinding<MaxCulBindingProvider> implements ManagedService, CULListener, MaxCulSender {
	public static final String MAXCUL_ADDRESS = "010203";
	public static final Object BROADCAST_ADDRESS = "000000";
	private String address = null;
	private Map<String, RoomConfig> roomMap = new HashMap<String, RoomConfig>();
	private CULHandler cul;
	private List<Sequence> sequences = Collections.synchronizedList(new ArrayList<Sequence>());
	private int msgCount = 0;
	private int credit10ms = Integer.MAX_VALUE;
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	
	private boolean listen;
	private boolean pair;
	
	private static final Logger logger = LoggerFactory.getLogger(MaxCulBinding.class);
	
	
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		Enumeration<String> enumeration = properties.keys();
		while (enumeration.hasMoreElements()) {
			String key = enumeration.nextElement();
			
			if (key.equals("address")) {
				this.address = (String) properties.get(key);
			} else if (key.startsWith("room")) {
				String[] roomSplit = key.split(":");
				String roomName = roomSplit[1];
				String type = roomSplit[2];
				if (!roomMap.containsKey(roomName)) {
					roomMap.put(roomName, new RoomConfig());
				}
				RoomConfig roomConfig = roomMap.get(roomName);
				roomConfig.setName(roomName);
				
				if (type.equals("group")) {
					roomConfig.setGroup(Integer.parseInt((String) properties.get(key)));
				} else if (type.equals("devices")) {
					String devicesAsString = (String) properties.get(key);
					for (String device : devicesAsString.split(",")) {
						roomConfig.getDeviceList().add(device.trim());	
					}
				}
			}
		}
		
		this.initCULHandler();
	}
	
	private void initCULHandler() {
		try {
			logger.debug("Opening CUL device on " + address);
			cul = CULManager.getOpenCULHandler(address, CULMode.MAX);
			cul.registerListener(this);
		} catch (CULDeviceException e) {
			logger.error("Can't open cul device", e);
			cul = null;
		}
	}
	
	@Override
	public void deactivate() {
		cul.unregisterListener(this);
		CULManager.close(cul);
	}
	
	private MaxCulBindingProvider getBindingProvider() {
		MaxCulBindingProvider provider = null;
		for (BindingProvider provider_ : this.providers) {
			if (provider_ instanceof MaxCulBindingProvider) {
				provider = (MaxCulBindingProvider) provider_;
			}
		}
		return provider;
	}
	
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		MaxCulBindingProvider provider = getBindingProvider();
		
		MaxCulFeatureConfig featureItemConfig = provider.getFeatureConfig(itemName);
		MaxCulDeviceConfig deviceItemConfig = provider.getDeviceConfig(itemName);
		if (featureItemConfig != null) {
			if (featureItemConfig.isListen()) {
				if (command == OnOffType.ON) {
					this.listen = true;
				} else if (command == OnOffType.OFF) {
					this.listen = false;
				}
			} else if (featureItemConfig.isPair()) {
				if (command == OnOffType.ON) {
					this.pair = true;
				} else if (command == OnOffType.OFF) {
					this.pair = false;
				}
			}
		} else if (deviceItemConfig != null) {
			
		}
	}

	@Override
	public void dataReceived(String data) {
		if (!data.startsWith("Z")) {
			logger.trace("message is not for us: {}", data);
			return;
		}
		
		BaseMsg msg = BaseMsg.parseMessage(data);
		if (msg == null) {
			logger.warn("cannot parse message: {}", data);
			return;
		}

		msg.printMessage();
		
		if (this.listen) {
			return;
		}
		
		if (msg instanceof PairPingMsg && this.pair) {
			final Sequence sequence = new PairingSequence(this, sequences);
			executor.submit(sequence);
			
			sequence.addSequenceListener(new SequenceListener() {
				@Override
				public void finished() {
					// pairing finished
					sequences.remove(sequence);
					String serialNumber = sequence.getSourceSerialNumber();
					MaxCulBindingProvider provider = getBindingProvider();
					MaxCulDeviceConfig config = provider.getConfigForSerialNumber(serialNumber);
					config.saveStoredConfig();
					
					// add link partner
					for (String room : roomMap.keySet()) {
						RoomConfig roomConfig = roomMap.get(room);
						if (roomConfig.isInRoom(serialNumber)) {
							List<String> otherDevices = roomConfig.getOtherDevices(serialNumber);
							for (String otherDevice : otherDevices) {
								MaxCulDeviceConfig otherDeviceConfig = getBindingProvider().getConfigForSerialNumber(otherDevice);
								if (otherDeviceConfig == null) {
									logger.warn("cannot find device for serial number: {}, missing item", otherDevice);
								} else {
									if (otherDeviceConfig.isPaired()) {
										{	// one side
											Sequence sequenceAddLinkPartner = new ServerInitiatedSequence(MaxCulBinding.this, sequences);
											executor.submit(sequenceAddLinkPartner);
											sequences.add(sequenceAddLinkPartner);
											sequenceAddLinkPartner.addMsg(new AddLinkPartnerMsg(getMessageNumber(), (byte) 0x00, (byte) 0x00, MAXCUL_ADDRESS, sequence.getSourceDeviceAddress(), otherDeviceConfig.getAddress(), otherDeviceConfig.getDevice()));
											sequenceAddLinkPartner.addSequenceListener(new SequenceListener() {
												@Override
												public void finished() {
													sequences.remove(sequence);
												}
		
												@Override
												public void timedOut() {
													sequences.remove(sequence);
												}
											});
										}
										{	// other side
											Sequence sequenceAddLinkPartner = new ServerInitiatedSequence(MaxCulBinding.this, sequences);
											executor.submit(sequenceAddLinkPartner);
											sequences.add(sequenceAddLinkPartner);
											sequenceAddLinkPartner.addMsg(new AddLinkPartnerMsg(getMessageNumber(), (byte) 0x00, (byte) 0x00, MAXCUL_ADDRESS, otherDeviceConfig.getAddress(), config.getAddress(), config.getDevice()));
											sequenceAddLinkPartner.addSequenceListener(new SequenceListener() {
												@Override
												public void finished() {
													sequences.remove(sequence);
												}
		
												@Override
												public void timedOut() {
													sequences.remove(sequence);
												}
											});
										}
									}
								}
							}
							
							
						}
					}
				}

				@Override
				public void timedOut() {
					sequences.remove(sequence);
				}
			});
			this.sequences.add(sequence);
			sequence.addMsg(msg);
		}
		
		Sequence openSequence = findOpenSequence(msg);
		if (openSequence != null) {
			openSequence.addMsg(msg);
		}
	}
	
	private Sequence findOpenSequence(BaseMsg msg) {
		for (Sequence sequence : this.sequences) {
			if (sequence.doesMsgMatch(msg)) {
				return sequence;
			}
		}
		return null;
	}

	public byte getMessageNumber() {
		this.msgCount += 1;
		this.msgCount &= 0xFF;
		if (this.msgCount == 0xFF) {
			this.msgCount = 0;
		}
		return (byte) this.msgCount;
	}

	@Override
	public void error(Exception e) {
		logger.error("error while cummunicating with cul", e);
	}

	@Override
	public void send(BaseMsg msg) {
		msg.setFastSend(true);
		logger.trace("requires credit: " + msg.requiredCredit());
		while (this.credit10ms < msg.requiredCredit()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
			// waiting for credits...
			this.credit10ms = this.cul.getCredit10ms();
		}
		try {
			this.cul.send(msg.rawMsg);
			msg.printMessage();
		} catch (CULCommunicationException e) {
			logger.error("error while sending", e);
		}
		
		this.credit10ms = this.cul.getCredit10ms();
	}

}
