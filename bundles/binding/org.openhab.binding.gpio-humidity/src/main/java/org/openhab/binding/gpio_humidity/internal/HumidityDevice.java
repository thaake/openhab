package org.openhab.binding.gpio_humidity.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.gpio_raspberry.device.I2CDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HumidityDevice extends I2CDevice<HumidityConfig, HumidityItemConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(HumidityDevice.class);
	
	private static final int measureDistance = 5000;

	public HumidityDevice(HumidityConfig config) {
		super(config);
	}

	@Override
	public State communicate(Command command, HumidityItemConfig itemConfig, State state) {
		LOG.debug("getting available data from device: " + config.getAddress());

		List<Long> results = new ArrayList<Long>();
		for (int i = 0; i < itemConfig.getMeasures(); i++) {
			super.open("/dev/i2c-1");
			if (itemConfig.getPort() != null) {
				super.write(itemConfig.getPort());
			}
	
			String str = new String(super.readAll());
			Long result = NumberUtils.toLong(str);
			LOG.debug("received value: " + result);
			results.add(result);
			
			super.close();
			
			try {
				Thread.sleep(measureDistance);
			} catch (InterruptedException e) {
				// no problem
			}
		}
		
		Double average = calcAverage(results);
		if (average == null) {
			return null;
		}
		
		double result = average;
		
		if (itemConfig.getWet() != null && itemConfig.getDry() != null) {
			double range = itemConfig.getDry() - itemConfig.getWet();
			double value = result - itemConfig.getWet();
			result = 100 - (long) ((value / range) * 100);
			
			if (result < 0) {
				result = 0l;
			}
			if (result > 100) {
				result = 100l;
			}
		}

		if (StringItem.class.isAssignableFrom(itemConfig.getItem().getClass())) {
			return new StringType(result + "");
		} else if (NumberItem.class
				.isAssignableFrom(itemConfig.getItem().getClass())) {
			return new DecimalType(result);
		} else {
			throw new IllegalStateException("item type unknown: "
					+ itemConfig.getItem());
		}
	}
	
	private static Double calcAverage(List<Long> results) {
		Collections.sort(results, new Comparator<Long>() {
			@Override
			public int compare(Long o1, Long o2) {
				return (int) (o1 - o2);
			}
		});
		
		if (results.size() == 0) {
			return null;
		}
		
		if (results.size() == 1) {
			return results.get(0).doubleValue();
		}
		
		if (results.size() == 2) {
			return (results.get(0) + results.get(1)) / 2d;
		}
		
		int center = results.size() / 2;
		double midPoint = results.get(center);
		if (results.size() % 2 == 0) {
			midPoint = (results.get(center) + results.get(center + 1)) / 2d;
		}
		
		if (results.size() < 5) {
			return midPoint;
		}
		
		int quartil = ((results.size() / 2) - (results.size() % 2)) / 1;
		
		double quartilDown = calcQuartil(results, quartil, quartil + quartil);
		double quartilUp = calcQuartil(results, results.size() - quartil - quartil, results.size() - quartil);
		double quartilDist = ((quartilUp - quartilDown) == 0 ? 1 : (quartilUp - quartilDown)) * 1.5d;
		
		return getAverage(results, quartilDown - quartilDist, quartilUp + quartilDist);
	}

	private static double getAverage(List<Long> results, double min, double max) {
		double res = 0;
		double count = 0;
		for (Long val : results) {
			if (val > min && val < max) {
				res += val;
				count++;
			}
		}
		return res / count;
	}

	private static double calcQuartil(List<Long> results, int from, int to) {
		double res = 0;
		for (int i = from; i < to; i++) {
			res = results.get(i);
		}
		return res / (to - from);
	}
}
