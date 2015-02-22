package org.openhab.binding.gpio_humidity.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TestHumidityDevice {
	

	@Test
	public void test() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		HumidityDevice humidityDevice = new HumidityDevice(new HumidityConfig("id", (byte) 0x22));
//		humidityDevice.communicate(null, new HumidityItemConfig((byte) 1, 400000l, 200000l, 1), null);
		
		//private static Double calcAverage(List<Long> results) {
		
		List<Long> results = new ArrayList<Long>();
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertNull(result);
	}
	
	@Test
	public void test2() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
	
	@Test
	public void test3() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
	
	@Test
	public void test4() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
	
	@Test
	public void test5() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
	
	@Test
	public void test6() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
	
	@Test
	public void test7() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Long> results = new ArrayList<Long>();
		results.add(100000l);
		results.add(200000l);
		results.add(100000l);
		results.add(10000l);
		results.add(100000l);
		
		Method method = HumidityDevice.class.getDeclaredMethod("calcAverage", List.class);
		method.setAccessible(true);
		Double result = (Double) method.invoke(null, results);
		Assert.assertEquals(100000l, result, 0.1);
	}
}
