package org.openhab.binding.gpio_mcp3008.internal;

import java.util.ArrayList;
import java.util.List;

import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.gpio_raspberry.device.SPIDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCP3008Device extends SPIDevice<MCP3008Config, MCP3008ItemConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(MCP3008Device.class);

	public MCP3008Device(MCP3008Config config) {
		super(config);
	}

	@Override
	public State communicate(Command command, MCP3008ItemConfig itemConfig, State state) {
		if (itemConfig.getFactor() == 0) {
			int maxAmplitude = 0;
			int minAmplitude = Integer.MAX_VALUE;
			long before = System.currentTimeMillis();
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < itemConfig.getMeterings(); i++) {
				int measured = this.measure(itemConfig.getPort(), true);
				if (measured > maxAmplitude) {
					maxAmplitude = measured;
				}
				if (measured < minAmplitude) {
					minAmplitude = measured;
				}
				list.add(measured);
			}
			long duration = System.currentTimeMillis() - before;
			Result result = normalize(list);
			LOG.info("item not properly configured, factor should not be '0'");
			LOG.info("measure time: " + duration + " (" + itemConfig.getMeterings() + " meterings)");
			LOG.info("----- ORIGINAL -----");
			LOG.info("maxAmplitude: " + maxAmplitude);
			LOG.info("minAmplitude: " + minAmplitude);
			LOG.info("average: " + ((minAmplitude + maxAmplitude) / 2));
			LOG.info("----- NORMALIZED -----");
			LOG.info("maxAmplitude: " + result.maxAmplitude);
			LOG.info("minAmplitude: " + result.minAmplitude);
			LOG.info("average: " + ((result.minAmplitude + result.maxAmplitude) / 2));
			return new DecimalType(0);
		} else {
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < itemConfig.getMeterings(); i++) {
				int measured = this.measure(itemConfig.getPort(), false);
				list.add(measured);
			}
			Result result = normalize(list);
			LOG.debug("measuring done, calculating with factor: " + itemConfig.getFactor());
			final int amplitudeSize = result.maxAmplitude - result.minAmplitude;
			LOG.trace("amplitude size: " + amplitudeSize);
			double finalValue = amplitudeSize * itemConfig.getFactor();
			LOG.trace("calculated value (with factor): " + finalValue);
			finalValue = finalValue + itemConfig.getOffset();
			LOG.trace("calculated value (with offset): " + finalValue);
			if (NumberItem.class.isAssignableFrom(itemConfig.getItem().getClass())) {
				return new DecimalType(finalValue);				
			} else if (StringItem.class.isAssignableFrom(itemConfig.getItem().getClass())) {
				return new StringType(finalValue + "");
			} else {
				throw new IllegalStateException("item type unknown: " + itemConfig.getItem().getClass());
			}
		}
	}
	
	static Result normalize(List<Integer> input) {
		double average = 0;
		for (Integer val : input) {
			average += val;
		}
		average = average / input.size();
		LOG.trace("av: " + average);
		double avMin = 0;
		int sizeMin = 0;
		double avMax = 0;
		int sizeMax = 0;
		for (Integer val : input) {
			if (val < average) {
				avMin += val;
				sizeMin++;
			} else if (val > average) {
				avMax += val;
				sizeMax++;
			}
		}
		avMin = avMin / sizeMin;
		avMax = avMax / sizeMax;
		LOG.trace("avMin: " + avMin);
		LOG.trace("avMax: " + avMax);
		
		int minAmpl = Integer.MAX_VALUE;
		int maxAmpl = 0;
		int newAverage = 0;
		int countValid = 0;
		
		for (int i = 0; i < input.size(); i++) {
			int val = input.get(i);
			double perc = 0;
			if (val < avMin) {
				perc = 100 - (val / avMin) * 100;
				
			} else if (val > avMax) {
				perc = 100 - (avMax / val) * 100;
			}
			if (perc < 5) {
				if (val < minAmpl) {
					minAmpl = val;
				}
				if (val > maxAmpl) {
					maxAmpl = val;
				}
				newAverage += val;
				countValid++;
			} else {
				LOG.trace("remove: " + val);
			}
		}
		newAverage = newAverage / countValid;
		LOG.trace("minAmpl: " + minAmpl);
		LOG.trace("maxAmpl: " + maxAmpl);
		LOG.trace("new Average: " + newAverage);
		Result res = new Result();
		res.minAmplitude = minAmpl;
		res.maxAmplitude = maxAmpl;
		return res;
	}
	
	private static class Result {
		public int minAmplitude;
		public int maxAmplitude;
	}
	
	private Integer measure(byte port, boolean print) {
		super.open();
		try {
			super.cs(true);
	
	        super.sclk(false);
	        super.cs(false);
	
	        int adccommand = port;
	        adccommand |= 0x18; // 0x18: 00011000
	        adccommand <<= 3;
	        // Send 5 bits: 8 - 3. 8 input channels on the MCP3008.
	        for (int i = 0; i < 5; i++) //
	        {
	            if ((adccommand & 0x80) != 0x0) // 0x80 = 0&10000000
	            	super.mosi(true);
	            else
	            	super.mosi(false);
	            adccommand <<= 1;
	            super.sclk(true);
	            super.sclk(false);
	        }
	
	        LOG.trace("reading data...");
	        int adcOut = 0;
	        for (int i = 0; i < 11; i++) // Read in one null bit and 10 ADC bits
	        {
	        	super.sclk(true);
	            super.sclk(false);
	            adcOut <<= 1;
	
	            if (super.isMiso()) {
	                adcOut |= 0x1;
	            }
	        }
	        super.cs(true);
	
	        LOG.trace("measured: " + adcOut);
	        return adcOut;
		} finally {
			super.close();
		}
	}

}
