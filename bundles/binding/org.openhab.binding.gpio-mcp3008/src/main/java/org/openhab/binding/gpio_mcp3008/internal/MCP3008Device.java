package org.openhab.binding.gpio_mcp3008.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	private static final float SINUS_CURVES = 1;
	private static final float NANOS_PER_SINUS = 20 * 1000 * 1000;
	private static final float MEASURE_POINTS_PER_SINUS = 100;
//	private static final int PART_OF_CURVE_FOR_PEAK_DETECTION = 13;
//	private static final int DIVERSIFICATION_POINTS = MEASURE_POINTS_PER_SINUS
//			/ PART_OF_CURVE_FOR_PEAK_DETECTION;
	private static final float AMPLITUDE_STEP_SIZE = 0.2f;

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
			long timeOnSinus = (long) ((now - ref) % (NANOS_PER_SINUS * SINUS_CURVES));
			long rasteredTimeOnSinus = (long) (timeOnSinus
					- (timeOnSinus % (NANOS_PER_SINUS / MEASURE_POINTS_PER_SINUS * SINUS_CURVES)));
//			long rasteredTimeOnSinus = now;
			if (!measures.containsKey(rasteredTimeOnSinus)) {
				measures.put(rasteredTimeOnSinus, new ArrayList<Integer>());
			}
			measures.get(rasteredTimeOnSinus).add(measured);
		}
		Map<Long, Float> measuresNormalized = new HashMap<Long, Float>();
		float yMovement = 0;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		for (Long time : measures.keySet()) {
			List<Integer> measurePointList = measures.get(time);
			float average = average(measurePointList);
			yMovement += average;
			if (average < min) {
				min = average;
			}
			if (average > max) {
				max = average;
			}
			
			measuresNormalized.put(time, average);
		}
		yMovement = yMovement / measuresNormalized.size();
		float amplHalf = Math.min(Math.abs(yMovement - max), Math.abs(yMovement - min));
		float xMovement = findBestXMovement(measuresNormalized, yMovement, amplHalf);
		float bestAmplitude = findBestAmplitude(measuresNormalized, yMovement, amplHalf, xMovement);

		LOG.trace("averageOfSinus: {}", yMovement);
		LOG.trace("amplHalf: {}", amplHalf);
		LOG.trace("bestXMovement: {}", xMovement);
		LOG.trace("bestAmplitude: {}", bestAmplitude);
		LOG.trace("measurePoints: {}", measures.size());
		
		List<Long> sortedTimes = new ArrayList<Long>(measuresNormalized.keySet());
		Collections.sort(sortedTimes, new Comparator<Long>() {
			@Override
			public int compare(Long arg0, Long arg1) {
				return (int) (arg0 - arg1);
			}
		});
		this.dumpCurve(measuresNormalized, sortedTimes);
		this.dumpOptimzedCurve(sortedTimes, 
				xMovement, bestAmplitude, yMovement);
		

//		float peakSpan = findPeaks(times, measuresNormalized, averageOverAll);

		LOG.debug("measuring done, calculating with factor: "
				+ itemConfig.getFactor());
		LOG.trace("amplitude size: " + bestAmplitude);
		double finalValue = bestAmplitude * itemConfig.getFactor();
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
	
	private void dumpOptimzedCurve(List<Long> times, float xMovement, float amplitude, float yMovement) {
		StringBuffer sb = new StringBuffer();
		for (Long time : times) {
			float sinusInput = (float) ((time / NANOS_PER_SINUS * SINUS_CURVES) * Math.PI);
			float sinusOffset = (xMovement / 20000000f);
			float optimizedValue = (float) (amplitude * Math.sin(sinusInput + sinusOffset) + yMovement);
			sb.append(time + ";" + optimizedValue).append(
					System.lineSeparator());
		}
		LOG.trace(sb.toString());
	}

	private void dumpCurve(Map<Long, Float> curve, List<Long> sortedTimes) {
		StringBuffer sb = new StringBuffer();
		for (Long time : sortedTimes) {
			sb.append(time + ";" + curve.get(time)).append(
					System.lineSeparator());
		}
		LOG.trace(sb.toString());
	}

	private float findBestAmplitude(Map<Long, Float> measuresNormalized,
			float averageOverAll, float amplHalf, float xMovement) {
		float bestDistance = this.calcDistance(measuresNormalized, averageOverAll, amplHalf, xMovement);
		float distanceBelow = this.calcDistance(measuresNormalized, averageOverAll, amplHalf - AMPLITUDE_STEP_SIZE, xMovement);
		float distanceAbove = this.calcDistance(measuresNormalized, averageOverAll, amplHalf + AMPLITUDE_STEP_SIZE, xMovement);
		float bestAmplitude = amplHalf;
		float currentStepSize = 0;
		if (distanceBelow < bestDistance) {
			LOG.trace("best amplitude is below: {}", amplHalf);
			currentStepSize = -AMPLITUDE_STEP_SIZE;
		} else if (distanceAbove < bestDistance) {
			LOG.trace("best amplitude is above: {}", amplHalf);
			currentStepSize = AMPLITUDE_STEP_SIZE;
		} else {
			return bestDistance;
		}
			
		for (float amplitude = amplHalf; ; amplitude += currentStepSize) {
			float distance = this.calcDistance(measuresNormalized, averageOverAll, amplitude, xMovement);
			LOG.trace("distance for amplitude ({}): {}", amplitude, distance);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestAmplitude = amplitude;
			} else {
				break;
			}
		}
		
		return bestAmplitude;
	}

	private float findBestXMovement(Map<Long, Float> measuresNormalized,
			float yMovement, float amplHalf) {
		float bestDistance = Integer.MAX_VALUE;
		float bestXMovement = 0;
		for (float xMovement = 0; xMovement < 2 * Math.PI; xMovement += Math.PI / MEASURE_POINTS_PER_SINUS) {
			float distance = this.calcDistance(measuresNormalized, yMovement, amplHalf, xMovement);
			LOG.trace("distance for x movement ({}): {}", xMovement, distance);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestXMovement = xMovement;
			}
		}
		return bestXMovement;
	}
	
	private float calcDistance(Map<Long, Float> measuresNormalized,
            float yMovement, float amplHalf, float xMovement) {
		float distanceOverAll = 0;
			for (Map.Entry<Long, Float> entry : measuresNormalized.entrySet()) {
			float sinusInput = (float) ((entry.getKey() / NANOS_PER_SINUS * SINUS_CURVES) * Math.PI);
			float optimizedValue = (float) (amplHalf * Math.sin(sinusInput + xMovement) + yMovement);
			float distancePart = Math.abs(100f - optimizedValue / entry.getValue() * 100f);
			distanceOverAll += distancePart;
		}
		distanceOverAll = distanceOverAll / measuresNormalized.size();
		return distanceOverAll;
	}

//	private static float findPeaks(List<Long> times,
//			Map<Long, Float> measuresNormalized,
//			float averageOverAll) {
//		float smallestDiversificationHigh = Integer.MAX_VALUE;
//		float smallestDiversificationLow = Integer.MAX_VALUE;
//		float highPeak = (int) averageOverAll;
//		float lowPeak = (int) averageOverAll;
//		
//		// find one peak and calc average
//		for (int i = 0; i < times.size() - DIVERSIFICATION_POINTS; i++) {
//			DiversivicationWithValue divValue = calculateDiversification(i,
//					times, measuresNormalized);
//			
//			if (divValue.diversification <= smallestDiversificationHigh
//					&& divValue.value > averageOverAll) {
//				smallestDiversificationHigh = divValue.diversification;
//				highPeak = divValue.value;	
//				LOG.trace("found lower diversification for high: {}", divValue);
//			}
//			if (divValue.diversification <= smallestDiversificationLow 
//					&& divValue.value < averageOverAll) {
//				smallestDiversificationLow = divValue.diversification;
//				lowPeak = divValue.value;
//				LOG.trace("found lower diversification for low: {}", divValue);
//			}
//		}
//
//		return highPeak - lowPeak;
//	}

//	private static DiversivicationWithValue calculateDiversification(int start,
//			List<Long> times, Map<Long, Float> measuresNormalized) {
//		float min = Integer.MAX_VALUE;
//		float max = Integer.MIN_VALUE;
//		float sum = 0;
//		for (int i = start; i < start + DIVERSIFICATION_POINTS; i++) {
//			Float value = measuresNormalized.get(times.get(i));
//			if (value < min)
//				min = value;
//			if (value > max)
//				max = value;
//			sum += value;
//		}
//		float average = sum / DIVERSIFICATION_POINTS;
//		float diff1 = Math.abs(average - min);
//		float diff2 = Math.abs(average - max);
//		return new DiversivicationWithValue(Math.min(diff1, diff2), average);
//	}

//	static class DiversivicationWithValue {
//		float diversification;
//		float value;
//
//		public DiversivicationWithValue(float diversification, float value) {
//			super();
//			this.diversification = diversification;
//			this.value = value;
//		}
//
//		@Override
//		public String toString() {
//			return "DiversivicationWithValue [diversification="
//					+ diversification + ", value=" + value + "]";
//		}
//		
//		
//	}

	private static float average(List<Integer> list) {
		float sum = 0;
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
