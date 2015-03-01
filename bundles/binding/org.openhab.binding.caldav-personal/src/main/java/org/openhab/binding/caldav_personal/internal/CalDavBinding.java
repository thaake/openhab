/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.caldav_personal.internal;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.caldav_personal.CalDavBindingProvider;
import org.openhab.binding.caldav_personal.internal.CalDavConfig.Type;

import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.caldav.CalDavEvent;
import org.openhab.io.caldav.CalDavLoader;
import org.openhab.io.caldav.EventNotifier;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author Robert Delbrück
 * @since 1.6.1
 */
public class CalDavBinding extends AbstractBinding<CalDavBindingProvider> implements ManagedService, EventNotifier {

	private static final String PARAM_HOME_IDENTIFIERS = "homeIdentifiers";
    private static final String PARAM_USED_CALENDARS = "usedCalendars";
	private static final DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	private static final Logger logger = 
		LoggerFactory.getLogger(CalDavBinding.class);
	
	private CalDavLoader calDavLoader;
	
    private List<String> calendars = new ArrayList<String>();
	private List<String> homeIdentifier = new ArrayList<String>();
	
	private ConcurrentHashMap<String, CalDavEvent> eventMap = new ConcurrentHashMap<String, CalDavEvent>();
	
	public CalDavBinding() {
	}
	
	public void setCalDavLoader(CalDavLoader calDavLoader) {
		this.calDavLoader = calDavLoader;
		this.calDavLoader.addListener(this);
	}
	
	public void unsetCalDavLoader() {
		this.calDavLoader.removeListener(this);
		this.calDavLoader = null;
	}
	
	public void activate() {
		
	}
	
	public void deactivate() {
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}

	
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		if (properties == null) {
			logger.warn("no configuration found");
		} else {
            String usedCalendars = (String) properties.get(PARAM_USED_CALENDARS);
            if (usedCalendars != null) {
                for (String cal : usedCalendars.split(",")) {
                    this.calendars.add(cal.trim());
                }
            }

			String homeIdentifiers = (String) properties.get(PARAM_HOME_IDENTIFIERS);
			if (homeIdentifiers != null) {
				for (String homeIdent : homeIdentifiers.split(",")) {
					this.homeIdentifier.add(homeIdent.trim().toLowerCase());
				}
			}
		}
	}
	
	@Override
	public void allBindingsChanged(BindingProvider provider) {
		this.updateItemsForEvent();
	}

	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		this.updateItemsForEvent();
	}

	@Override
	public void eventRemoved(CalDavEvent event) {
        if (!calendars.contains(event.getCalendarId())) {
            return;
        }

		this.eventMap.remove(event.getId());
		
		logger.debug("remove event from map: {}", event.getShortName());
		
		this.updateItemsForEvent();
	}

	@Override
	public void eventLoaded(CalDavEvent event) {
        if (!calendars.contains(event.getCalendarId())) {
            return;
        }

		this.eventMap.put(event.getId(), event);
		
		logger.debug("adding event to map: {}", event.getShortName());
		
		this.updateItemsForEvent();
	}

	@Override
	public void eventBegins(CalDavEvent event) {
        if (!calendars.contains(event.getCalendarId())) {
            return;
        }

		this.eventMap.put(event.getId(), event);
		
		logger.debug("adding event to map: {}", event.getShortName());
		
		this.updateItemsForEvent();
	}

	@Override
	public void eventEnds(CalDavEvent event) {
        if (!calendars.contains(event.getCalendarId())) {
            return;
        }

		this.eventMap.remove(event.getId());
		
		logger.debug("remove event from map: {}", event.getShortName());
		
		this.updateItemsForEvent();
	}
	
	private void updateItemsForEvent() {
		CalDavBindingProvider bindingProvider = null;
		for (CalDavBindingProvider bindingProvider_ : this.providers) {
			bindingProvider = bindingProvider_;
		}
		if (bindingProvider == null) {
			logger.error("no binding provider found");
			return;
		}
		
		for (String item : bindingProvider.getItemNames()) {
			CalDavConfig config = bindingProvider.getConfig(item);
			this.updateItem(item, config);
		}
	}
	
	private synchronized void updateItem(String itemName, CalDavConfig config) {
		if (config.getType() == Type.PRESENCE) {
			List<CalDavEvent> subList = getActiveEvents(config.getCalendar());
			subList = this.removeWithMatchingPlace(subList);
			if (subList.size() == 0) {
				eventPublisher.sendCommand(itemName, OnOffType.OFF);
			} else {
				eventPublisher.sendCommand(itemName, OnOffType.ON);
			}
		} else {
			List<CalDavEvent> subList = new ArrayList<CalDavEvent>();
			if (config.getType() == Type.EVENT) {
				subList = getAllEvents(config.getCalendar());
			} else if (config.getType() == Type.ACTIVE) {
				subList = getActiveEvents(config.getCalendar());
			} else if (config.getType() == Type.UPCOMING) {
				subList = getUpcomingEvents(config.getCalendar());
			}
			
			if (config.getEventNr() > subList.size()) {
				logger.debug("no event found for {}, setting to UNDEF", itemName);
				eventPublisher.postUpdate(itemName, org.openhab.core.types.UnDefType.UNDEF);
				return;
			}
			
			CalDavEvent event = subList.get(config.getEventNr() - 1);
			logger.trace("found event {} for config {}", event.getShortName(), config);
			State command = null;
			
			switch (config.getValue()) {
			case NAME: command = new StringType(event.getName()); break;
			case DESCRIPTION: command = new StringType(event.getContent()); break;
			case PLACE: command = new StringType(event.getLocation()); break;
			case START: 
				Calendar cal = Calendar.getInstance();
				cal.setTime(event.getStart());
				command = new DateTimeType(cal); 
				break;
			case END: 
				Calendar cal2 = Calendar.getInstance();
				cal2.setTime(event.getEnd());
				command = new DateTimeType(cal2); 
				break;
			case TIME:
				String startEnd = DF.format(event.getStart()) + " - " + DF.format(event.getEnd());
				command = new StringType(startEnd);
				break;
			default:
				logger.error("unhandled value: {}", config.getValue());
			}
			
			logger.debug("sending command {} for item {}", command, itemName);
			eventPublisher.postUpdate(itemName, command);
			logger.trace("command {} successfuly send", command);
		}
		
		
	}
	
	private List<CalDavEvent> getActiveEvents(String calendar) {
		List<CalDavEvent> subList = new ArrayList<CalDavEvent>();
		Date now = new Date();
		for (CalDavEvent event : this.eventMap.values()) {
			if (!event.getCalendarId().equals(calendar)) {
				continue;
			}
			
			if (!event.getStart().before(now)
					&& !event.getEnd().after(now)) {
				continue;
			}
			
			subList.add(event);
		}
		Collections.sort(subList, new MyComparator());
		return subList;
	}
	
	private List<CalDavEvent> getUpcomingEvents(String calendar) {
		List<CalDavEvent> subList = new ArrayList<CalDavEvent>();
		Date now = new Date();
		for (CalDavEvent event : this.eventMap.values()) {
			if (!event.getCalendarId().equals(calendar)) {
				continue;
			}
			
			if (event.getStart().before(now)) {
				continue;
			}
			
			subList.add(event);
		}
		Collections.sort(subList, new MyComparator());
		return subList;
	}
	
	private List<CalDavEvent> getAllEvents(String calendar) {
		List<CalDavEvent> subList = new ArrayList<CalDavEvent>();
		for (CalDavEvent event : this.eventMap.values()) {
			if (!event.getCalendarId().equals(calendar)) {
				continue;
			}
			
			subList.add(event);
		}
		Collections.sort(subList, new MyComparator());
		return subList;
	}
	
	private List<CalDavEvent> removeWithMatchingPlace(List<CalDavEvent> list) {
		List<CalDavEvent> out = new ArrayList<CalDavEvent>();
		for (CalDavEvent event : list) {
			if (!this.homeIdentifierMatch(event.getLocation())) {
				continue;
			}
			out.add(event);
		}
		return out;
	}
	
	private boolean homeIdentifierMatch(String location) {
		if (location == null) {
			return false;
		}
		
		boolean match = this.homeIdentifier.contains(location.toLowerCase());
		if (match) {
			return true;
		}
		
		for (String homeIdentifier : this.homeIdentifier) {
			if (location.contains(homeIdentifier)) {
				return true;
			}
		}
		
		return false;
	}
	
	class MyComparator implements Comparator<CalDavEvent> {
		@Override
		public int compare(CalDavEvent arg0, CalDavEvent arg1) {
			return arg0.getStart().compareTo(arg1.getStart());
		}
	}

}
