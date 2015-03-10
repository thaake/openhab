package org.openhab.binding.maxcul.internal.config;

public class MaxCulFeatureConfig extends MaxCulItemConfig {
	private boolean listen;
	private boolean pair;

	public boolean isListen() {
		return listen;
	}

	public void setListen(boolean listen) {
		this.listen = listen;
	}

	public boolean isPair() {
		return pair;
	}

	public void setPair(boolean pair) {
		this.pair = pair;
	}
	
	
}
