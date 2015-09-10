package org.openhab.binding.lirc.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LircTransceiver {
	private static final Logger LOG = LoggerFactory.getLogger(LircTransceiver.class);
	private static final String PATTERN = "([0-9a-zA-Z]+) ([0-9]{2}) ([0-9a-zA-Z-_]+) ([0-9a-zA-Z-_]+)";
	
	private List<LircEventListener> listenerList = new ArrayList<LircTransceiver.LircEventListener>();
	private Queue<LircBindingConfig> sendQueue = new LinkedBlockingQueue<LircBindingConfig>();
	
	private Thread listenerThread;
	private Thread senderThread;
	
	private String host;
	private int port;
	
	private BufferedInputStream bis;
	private BufferedOutputStream bos;
	private Socket socket;
	
	public LircTransceiver(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public boolean connect() {
		try {
			try {
				socket = new Socket(host, port);
			} catch (ConnectException e) {
				LOG.error(String.format("cannot create connection to LIRC (with host %s and port %s): %s", host, port, e.getMessage()), e);
				socket = null;
				return false;
			}
			bis = new BufferedInputStream(socket.getInputStream());
			bos = new BufferedOutputStream(socket.getOutputStream());
		} catch(IOException e) {
			LOG.error("cannot open socket: " + e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	public void disconnect() {
		if (this.socket != null) {
			if (this.senderThread != null) {
				this.senderThread.interrupt();
				this.senderThread = null;
			}
			if (this.listenerThread != null) {
				this.listenerThread.interrupt();
				this.listenerThread = null;
			}
			try {
				this.socket.close();
			} catch (IOException e) {
				LOG.error("cannot close socket", e);
			}
			this.socket = null;
		}
	}
	
	public void addListener(LircEventListener l) {
		this.listenerList.add(l);
	}
	
	public void removeListener(LircEventListener l) {
		this.listenerList.remove(l);
	}
	
	public void send(LircBindingConfig config) {
		this.sendQueue.add(config);
		LOG.trace("command added to queue '{}'", config);
	}
	
	public static interface LircEventListener {
		public void received(ReceivedData data);
	}
	
	public static class ReceivedData {
		private String device;
		private String command;
		
		public ReceivedData(String device, String command) {
			super();
			this.device = device;
			this.command = command;
		}

		public String getDevice() {
			return device;
		}
		
		public void setDevice(String device) {
			this.device = device;
		}
		
		public String getCommand() {
			return command;
		}
		
		public void setCommand(String command) {
			this.command = command;
		}
		
		
	}

	public boolean isConnected() {
		return this.socket != null;
	}

	public void startListener() {
		this.listenerThread = new Thread() {
			@Override
			public void run() {
				LOG.debug("receiver thread is running...");
				
				while (!isInterrupted()) {
					try {
						byte[] buffer = new byte[100];
						int length = bis.read(buffer);
						buffer = Arrays.copyOfRange(buffer, 0, length);
						String data = new String(buffer);
						
						Pattern pattern = Pattern.compile(PATTERN);
						Matcher matcher = pattern.matcher(data);
						
						String command = null;
						String device = null;
						
						if (matcher.find()) {
							command = matcher.group(3);
							device = matcher.group(4);
						} else {
							LOG.trace("data '{}' does not match", data);
							continue;
						}
						
						for (LircEventListener l : listenerList) {
							l.received(new ReceivedData(device, command));
						}
					} catch (UnknownHostException e) {
						LOG.error("cannot read data", e);
					} catch (IOException e) {
						LOG.error("cannot send data", e);
						LOG.info("restarting sender...");
						connect();
					}
				}
				
				try {
					socket.close();
				} catch (IOException e) { }
				
			}
			
		};
		this.listenerThread.start();
	}

	public void startSender() {
		this.senderThread = new Thread() {
			@Override
			public void run() {
				LOG.debug("sender thread is running...");
				while (!isInterrupted()) {
					if (!isConnected()) {
						LOG.warn("lirc is not connected");
					} else {
						if (!sendQueue.isEmpty()) {
							LircBindingConfig config = sendQueue.poll();
							if (config == null) {
								LOG.warn("config is null, cannot send");
								continue;
							}
							
							try {
								StringBuffer sb = new StringBuffer();
								sb.append("SEND_ONCE ");
								sb.append(config.getDevice() + " ");
								sb.append(config.getCommand());
								sb.append("\n");
								LOG.debug("sending data: " + sb.toString());
								bos.write(sb.toString().getBytes("UTF-8"));
								bos.flush();
							} catch (IOException e) {
								LOG.error("cannot send data", e);
								LOG.info("restarting sender...");
								sendQueue.add(config);
								connect();
							}
						} else {
							LOG.trace("send queue is empty");
						}
					}
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		this.senderThread.start();
	}
}
