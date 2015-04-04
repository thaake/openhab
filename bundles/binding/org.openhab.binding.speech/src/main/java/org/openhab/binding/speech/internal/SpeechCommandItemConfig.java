package org.openhab.binding.speech.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;

/**
 * @author Robert Delbrück
 * @since 1.7.0
 *
 */
public class SpeechCommandItemConfig implements SpeechItemConfig {
	private String primaryName;
	private List<String> alternativeNames = new ArrayList<String>();
	private Map<String, String> commands = new HashMap<String, String>();
	private Item item;
	private Type type;

	public SpeechCommandItemConfig() {
		super();
	}

	public SpeechCommandItemConfig(Item item, String primaryName, List<String> alternativeNames) {
		super();
		this.item = item;
		this.primaryName = primaryName;
		this.alternativeNames = alternativeNames;
	}

	public String getPrimaryName() {
		return primaryName;
	}

	public void setPrimaryName(String primaryName) {
		this.primaryName = primaryName;
	}

	public boolean isGroup() {
		return this.item instanceof GroupItem;
	}

	public List<String> getAlternativeNames() {
		return alternativeNames;
	}

	public void setAlternativeNames(List<String> alternativeNames) {
		this.alternativeNames = alternativeNames;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}
	

	public Map<String, String> getCommands() {
		return commands;
	}

	public void setCommands(Map<String, String> commands) {
		this.commands = commands;
	}
	
	


	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "SpeechCommandItemConfig [primaryName=" + primaryName
				+ ", alternativeNames=" + alternativeNames + ", commands="
				+ commands + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item == null) ? 0 : item.hashCode());
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
		SpeechCommandItemConfig other = (SpeechCommandItemConfig) obj;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		return true;
	}
	
	
	
}
