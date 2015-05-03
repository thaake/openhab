package org.openhab.binding.gpio_mcp3008.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private static final Logger LOG = LoggerFactory
			.getLogger(MCP3008Device.class);

	private static final int SINUS_CURVES = 2;
	private static final int NANOS_PER_SINUS = 20 * 1000 * 1000;
	private static final int MEASURE_POINTS_PER_SINUS = 120;
	private static final int PART_OF_CURVE_FOR_PEAK_DETECTION = 20;
	private static final int DIVERSIFICATION_POINTS = MEASURE_POINTS_PER_SINUS
			/ PART_OF_CURVE_FOR_PEAK_DETECTION;

	public MCP3008Device(MCP3008Config config) {
		super(config);
	}

	@Override
	public State communicate(Command command, MCP3008ItemConfig itemConfig,
			State state) {
		Map<Long, List<Integer>> measures = new HashMap<Long, List<Integer>>();
		long ref = System.nanoTime();
		for (int i = 0; i < itemConfig.getMeterings(); i++) {
			long now = System.nanoTime();
			int measured = this.measure(itemConfig.getPort(), false);
			long timeOnSinus = (now - ref) % (NANOS_PER_SINUS * SINUS_CURVES);
			long rasteredTimeOnSinus = timeOnSinus
					- (timeOnSinus % (NANOS_PER_SINUS / MEASURE_POINTS_PER_SINUS * SINUS_CURVES));
//			long rasteredTimeOnSinus = now;
			if (!measures.containsKey(rasteredTimeOnSinus)) {
				measures.put(rasteredTimeOnSinus, new ArrayList<Integer>());
			}
			measures.get(rasteredTimeOnSinus).add(measured);
		}
		Map<Long, Integer> measuresNormalized = new HashMap<Long, Integer>();
		for (Long time : measures.keySet()) {
			List<Integer> measurePointList = measures.get(time);
			int average = average(measurePointList);
			measuresNormalized.put(time, average);
		}

		LOG.trace("measurePoints: " + measures.size());
		List<Long> times = new ArrayList<Long>(measuresNormalized.keySet());
		Collections.sort(times, new Comparator<Long>() {
			@Override
			public int compare(Long arg0, Long arg1) {
				return (int) (arg0 - arg1);
			}
		});
		StringBuffer sb = new StringBuffer();
		for (Long time : times) {
			sb.append(time + ";" + measuresNormalized.get(time)).append(
					System.lineSeparator());
		}
		// try {
		// File path = new File("etc" + File.pathSeparatorChar + "mcp3008");
		// path.mkdirs();
		// File file = new File(path, "measures-" + System.currentTimeMillis() +
		// ".txt");
		// file.createNewFile();
		// FileOutputStream fis = new FileOutputStream(file);
		// IOUtils.write(sb.toString(), fis);
		// } catch (FileNotFoundException e) {
		// LOG.error("error writing measure points", e);
		// } catch (IOException e) {
		// LOG.error("error writing measure points", e);
		// }
		LOG.trace(sb.toString());

		int peakSpan = findPeaks(times, measuresNormalized);

		LOG.debug("measuring done, calculating with factor: "
				+ itemConfig.getFactor());
		LOG.trace("amplitude size: " + peakSpan);
		double finalValue = peakSpan * itemConfig.getFactor();
		LOG.trace("calculated value (with factor): " + finalValue);
		finalValue = finalValue + itemConfig.getOffset();
		LOG.trace("calculated value (with offset): " + finalValue);
		if (NumberItem.class.isAssignableFrom(itemConfig.getItem().getClass())) {
			return new DecimalType(finalValue);
		} else if (StringItem.class.isAssignableFrom(itemConfig.getItem()
				.getClass())) {
			return new StringType(finalValue + "");
		} else {
			throw new IllegalStateException("item type unknown: "
					+ itemConfig.getItem().getClass());
		}
	}

	private static int findPeaks(List<Long> times,
			Map<Long, Integer> measuresNormalized) {
		int smallestDiversificationHigh = Integer.MAX_VALUE;
		int smallestDiversificationLow = Integer.MAX_VALUE;
		int highPeak = Integer.MIN_VALUE;
		int lowPeak = Integer.MAX_VALUE;
		for (int i = 0; i < times.size() - DIVERSIFICATION_POINTS; i++) {
			DiversivicationWithValue divValue = calculateDiversification(i,
					times, measuresNormalized);
			if (smallestDiversificationHigh == Integer.MAX_VALUE) {
				smallestDiversificationHigh = divValue.diversification;
				highPeak = divValue.value;
				LOG.trace("setting high to initial: {}", divValue);
			}
			if (smallestDiversificationLow == Integer.MAX_VALUE) {
				smallestDiversificationLow = divValue.diversification;
				lowPeak = divValue.value;
				LOG.trace("setting low to initial: {}", divValue);
			}
			if (divValue.diversification < smallestDiversificationHigh
					&& divValue.value > lowPeak) {
				smallestDiversificationHigh = divValue.diversification;
				highPeak = divValue.value;	
				LOG.trace("found lower diversification for high: {}", divValue);
			}
			if (divValue.diversification < smallestDiversificationLow 
					&& divValue.value < highPeak) {
				smallestDiversificationLow = divValue.diversification;
				lowPeak = divValue.value;
				LOG.trace("found lower diversification for low: {}", divValue);
			}
		}

		return highPeak - lowPeak;
	}

	private static DiversivicationWithValue calculateDiversification(int start,
			List<Long> times, Map<Long, Integer> measuresNormalized) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int sum = 0;
		for (int i = start; i < start + DIVERSIFICATION_POINTS; i++) {
			Integer value = measuresNormalized.get(times.get(i));
			if (value < min)
				min = value;
			if (value > max)
				max = value;
			sum += value;
		}
		int average = sum / DIVERSIFICATION_POINTS;
		int diff1 = Math.abs(average - min);
		int diff2 = Math.abs(average - max);
		return new DiversivicationWithValue(Math.min(diff1, diff2), average);
	}

	static class DiversivicationWithValue {
		int diversification;
		int value;

		public DiversivicationWithValue(int diversification, int value) {
			super();
			this.diversification = diversification;
			this.value = value;
		}

		@Override
		public String toString() {
			return "DiversivicationWithValue [diversification="
					+ diversification + ", value=" + value + "]";
		}
		
		
	}

	private static int average(List<Integer> list) {
		int sum = 0;
		for (Integer i : list) {
			sum += i;
		}
		return sum / list.size();
	}

	// static Result normalize(List<Integer> input) {
	// double average = 0;
	// for (Integer val : input) {
	// average += val;
	// }
	// average = average / input.size();
	// LOG.trace("av: " + average);
	// double avMin = 0;
	// int sizeMin = 0;
	// double avMax = 0;
	// int sizeMax = 0;
	// for (Integer val : input) {
	// if (val < average) {
	// avMin += val;
	// sizeMin++;
	// } else if (val > average) {
	// avMax += val;
	// sizeMax++;
	// }
	// }
	// avMin = avMin / sizeMin == 0 ? 1 : sizeMin;
	// avMax = avMax / sizeMax == 0 ? 1 : sizeMax;
	// LOG.trace("avMin: " + avMin);
	// LOG.trace("avMax: " + avMax);
	//
	// int minAmpl = Integer.MAX_VALUE;
	// int maxAmpl = 0;
	// int newAverage = 0;
	// int countValid = 0;
	//
	// for (int i = 0; i < input.size(); i++) {
	// int val = input.get(i);
	// double perc = 0;
	// if (val < avMin) {
	// perc = 100 - (val / avMin) * 100;
	//
	// } else if (val > avMax) {
	// perc = 100 - (avMax / val) * 100;
	// }
	// if (perc < 5) {
	// if (val < minAmpl) {
	// minAmpl = val;
	// }
	// if (val > maxAmpl) {
	// maxAmpl = val;
	// }
	// newAverage += val;
	// countValid++;
	// } else {
	// LOG.trace("remove: " + val);
	// }
	// }
	// newAverage = newAverage / countValid;
	// LOG.trace("minAmpl: " + minAmpl);
	// LOG.trace("maxAmpl: " + maxAmpl);
	// LOG.trace("new Average: " + newAverage);
	// Result res = new Result();
	// res.minAmplitude = minAmpl;
	// res.maxAmplitude = maxAmpl;
	// return res;
	// }

	// private static class Result {
	// public int minAmplitude;
	// public int maxAmplitude;
	// }

	private Integer measure(byte port, boolean print) {
		super.open();
		try {
			this.cs(true);

			this.sclk(false);
			this.cs(false);

			int adccommand = port;
			adccommand |= 0x18;
			adccommand <<= 3;
			for (int i = 0; i < 5; i++) {
				if ((adccommand & 0x80) != 0) {
					this.mosi(true);
				} else {
					this.mosi(false);
				}
				adccommand <<= 1;
				this.sclk(true);
				this.sclk(false);
			}
//			LOG.trace("reading data...");
			int adcOut = 0;
			for (int i = 0; i < 11; i++) {
				this.sclk(true);
				this.sclk(false);
				adcOut <<= 1;

				if (this.isMiso()) {
					adcOut |= 0x1;
				}
			}
			this.cs(true);

//			LOG.trace("measured: " + adcOut);
			return adcOut;
		} finally {
			super.close();
		}
	}

}
