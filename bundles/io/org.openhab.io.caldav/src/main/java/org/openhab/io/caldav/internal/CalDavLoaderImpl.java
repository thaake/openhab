/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.caldav.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.openhab.core.service.AbstractActiveService;
import org.openhab.io.caldav.CalDavEvent;
import org.openhab.io.caldav.CalDavLoader;
import org.openhab.io.caldav.EventNotifier;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

/**
 * Default implementation of the CalDAV loader.
 * 
 * @author Robert Delbrück
 * @since 1.7.0
 * 
 */
public class CalDavLoaderImpl extends AbstractActiveService implements
		ManagedService, CalDavLoader {
	private static final String PROP_RELOAD_INTERVAL = "reloadInterval";
	private static final String PROP_PRELOAD_TIME = "preloadTime";
	private static final String PROP_URL = "url";
	private static final String PROP_PASSWORD = "password";
	private static final String PROP_USERNAME = "username";

	private static final Logger LOG = LoggerFactory
			.getLogger(CalDavLoaderImpl.class);

	private Map<String, CalDavConfig> configMap = new HashMap<String, CalDavConfig>();

	private ConcurrentHashMap<String, Map<String, CalDavEvent>> eventCache = new ConcurrentHashMap<String, Map<String, CalDavEvent>>();
	private ConcurrentHashMap<String, TimerTask> timerBeginMap = new ConcurrentHashMap<String, TimerTask>();
	private ConcurrentHashMap<String, TimerTask> timerEndMap = new ConcurrentHashMap<String, TimerTask>();
	private List<EventNotifier> eventListenerList = new ArrayList<EventNotifier>();

	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		if (config != null) {
			Enumeration<String> iter = config.keys();
			while (iter.hasMoreElements()) {
				String key = iter.nextElement();
				LOG.trace("configuration parameter: " + key);
				if (key.equals("service.pid")) {
					continue;
				}
				String[] keys = key.split(":");
				String id = keys[0];
				String paramKey = keys[1];
				CalDavConfig calDavConfig = configMap.get(id);
				if (calDavConfig == null) {
					calDavConfig = new CalDavConfig();
					configMap.put(id, calDavConfig);
				}
				String value = config.get(key) + "";

				calDavConfig.setKey(id);
				if (paramKey.equals(PROP_USERNAME)) {
					calDavConfig.setUsername(value);
				} else if (paramKey.equals(PROP_PASSWORD)) {
					calDavConfig.setPassword(value);
				} else if (paramKey.equals(PROP_URL)) {
					calDavConfig.setUrl(value);
				} else if (paramKey.equals(PROP_RELOAD_INTERVAL)) {
					calDavConfig.setReloadMinutes(Integer.parseInt(value));
				} else if (paramKey.equals(PROP_PRELOAD_TIME)) {
					calDavConfig.setPreloadMinutes(Integer.parseInt(value));
				}
			}

			for (String id : this.configMap.keySet()) {
				if (configMap.get(id).getUrl() == null) {
					throw new ConfigurationException(PROP_URL, PROP_URL + " must be set");
				}
				if (configMap.get(id).getUsername() == null) {
					throw new ConfigurationException(PROP_USERNAME, PROP_USERNAME + " must be set");
				}
				if (configMap.get(id).getPassword() == null) {
					throw new ConfigurationException(PROP_PASSWORD, PROP_PASSWORD + " must be set");
				}
				if (configMap.get(id).getReloadMinutes() == 0) {
					throw new ConfigurationException(PROP_RELOAD_INTERVAL, PROP_RELOAD_INTERVAL + " must be set");
				}
				if (configMap.get(id).getPreloadMinutes() == 0) {
					throw new ConfigurationException(PROP_PRELOAD_TIME, PROP_PRELOAD_TIME + " must be set");
				}
				LOG.trace("config for id '{}': {}", id, configMap.get(id));
			}

			setProperlyConfigured(true);
			this.startLoading();
		} else {
			LOG.warn("configuration not found");
		}
	}

	public void addListener(EventNotifier notifier) {
		this.eventListenerList.add(notifier);
	}

	public void removeListener(EventNotifier notifier) {
		this.eventListenerList.remove(notifier);
	}

	private void addEvent(CalDavEvent event) {
		if (!this.eventCache.containsKey(event.getCalendarId())) {
			LOG.debug("creating event map for calendar: {}", event.getCalendarId());
			this.eventCache.put(event.getCalendarId(), new HashMap<String, CalDavEvent>());
		}
		
		Map<String, CalDavEvent> eventMap = this.eventCache.get(event.getCalendarId());
		
		if (eventMap.containsKey(event.getId())) {
			if (event.getLastChanged().after(eventMap.get(event.getId()).getLastChanged())) {
				LOG.debug("event is already in event map and newer -> delete the old one, reschedule timer");
				// cancel old jobs
				if (timerBeginMap.contains(event.getId())) {
					timerBeginMap.get(event.getId()).cancel();
					timerBeginMap.remove(event.getId());
				}
				if (timerEndMap.contains(event.getId())) {
					timerEndMap.get(event.getId()).cancel();
					timerEndMap.remove(event.getId());
				}
				
				// override event
				eventMap.remove(event.getId());
				eventMap.put(event.getId(), event);
				
				for (EventNotifier notifier : eventListenerList) {
					notifier.eventRemoved(event);
					notifier.eventLoaded(event);
				}
				
				createJob(event);
			} else {
				// event is already in map and not updated, ignoring
			}
		} else {
			eventMap.put(event.getId(), event);
			for (EventNotifier notifier : eventListenerList) {
				notifier.eventLoaded(event);
				
			}
			createJob(event);
		}
	}
	
	private void createJob(final CalDavEvent event) {
		TimerTask timerTaskBegin = new TimerTask() {
			@Override
			public void run() {
				LOG.info("event start for: {}", event.getShortName());
				for (EventNotifier notifier : eventListenerList) {
					notifier.eventBegins(event);
				}
				timerBeginMap.get(event.getId()).cancel();
				timerBeginMap.remove(event.getId());
			}
		};
		timerBeginMap.put(event.getId(), timerTaskBegin);
		new Timer().schedule(timerTaskBegin, event.getStart());
		LOG.debug("begin timer scheduled for event '{}' @ {}", event.getShortName(), event.getStart());
		
		TimerTask timerTaskEnd = new TimerTask() {
			@Override
			public void run() {
				LOG.info("event end for: {}", event.getShortName());
				for (EventNotifier notifier : eventListenerList) {
					notifier.eventEnds(event);
				}
				timerEndMap.get(event.getId()).cancel();
				timerEndMap.remove(event.getId());
				
				eventCache.get(event.getCalendarId()).remove(event.getId());
			}
		};
		timerEndMap.put(event.getId(), timerTaskEnd);
		new Timer().schedule(timerTaskEnd, event.getEnd());
		LOG.debug("end timer scheduled for event '{}' @ {}", event.getShortName(), event.getEnd());
	}

	private List<CalDavEvent> loadEvents(CalDavConfig config)
			throws IOException, ParserException {
		List<CalDavEvent> eventList = new ArrayList<CalDavEvent>();

		Sardine sardine = SardineFactory.begin(config.getUsername(),
				config.getPassword());

		CompatibilityHints.setHintEnabled(
				CompatibilityHints.KEY_RELAXED_PARSING, true);

		List<DavResource> list = sardine.list(config.getUrl());

		for (DavResource resource : list) {
			if (resource.isDirectory()) {
				continue;
			}

			URL url = new URL(config.getUrl());
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
					resource.getPath());

			InputStream inputStream = sardine.get(url.toString().replaceAll(
					" ", "%20"));

			CalendarBuilder builder = new CalendarBuilder();

			Calendar calendar = builder.build(inputStream);
			for (CalendarComponent comp : calendar.getComponents()) {
				if (comp instanceof VEvent) {
					VEvent vEvent = (VEvent) comp;
					// System.out.println(vEvent.getSummary());
					java.util.Calendar instance1 = java.util.Calendar
							.getInstance();
					instance1.add(java.util.Calendar.HOUR_OF_DAY, -1);
					Date startDate = instance1.getTime();

					PeriodList periods = vEvent
							.calculateRecurrenceSet(new Period(new DateTime(
									startDate), new Dur(0, 0, config.getPreloadMinutes(), 0)));

					for (Period p : periods) {
						if (p.getEnd().before(new Date())) {
							// date is already ended, ignoring
							continue;
						}
						CalDavEvent event = new CalDavEvent(vEvent.getSummary()
								.getValue(), vEvent.getUid().getValue(),
								config.getKey(), new Date(p.getStart()
										.getTime()), new Date(p.getEnd()
										.getTime()));
						event.setLastChanged(vEvent.getLastModified().getDate());
						if (vEvent.getLocation() != null) {
							event.setLocation(vEvent.getLocation().getValue());
						}
						if (vEvent.getDescription() != null) {
							event.setContent(vEvent.getDescription().getValue());
						}
						addEvent(event);
					}

				}
			}
		}

		return eventList;
	}
	
	

	public void startLoading() {
		if (timerBeginMap.size() > 0
				|| timerEndMap.size() > 0) {
			return;
		}
		LOG.trace("starting execution...");

		for (final CalDavConfig config : configMap.values()) {
			new Timer(true).schedule(new TimerTask() {

				@Override
				public void run() {

					Executors.newSingleThreadExecutor().submit(new Runnable() {
						@Override
						public void run() {
							try {
								LOG.debug("loading events for config: "
										+ config.getKey());
								loadEvents(config);
							} catch (IOException e) {
								LOG.error("error while loading calendar entries: " + e.getMessage(), e);
							} catch (ParserException e) {
								LOG.error("error while loading calendar entries: " + e.getMessage(), e);
							}
						}
					});

				}
			}, 100, config.getReloadMinutes() * 1000 * 60);
		}
	}

	@Override
	protected void execute() {
		
	}

	@Override
	protected long getRefreshInterval() {
		return 1000;
	}

	@Override
	protected String getName() {
		return "CalDav Loader";
	}

}
