/**
 * Copyright 2016 Symantec Corporation.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.api.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache.Entry;

import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.Ignition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.dao.PerformanceMonitor;
import com.srotya.tau.api.storage.Point;

/**
 * Tests for performance monitor
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPerformanceMonitor {

	private static Ignite ignite;
	@Mock
	private ApplicationManager am;

	@BeforeClass
	public static void beforeClass() {
		Ignition.setClientMode(false);
		ignite = Ignition.start();
	}

	@AfterClass
	public static void afterClass() {
		if (ignite != null) {
			ignite.close();
		}
	}

	@Before
	public void before() {
		when(am.getIgnite()).thenReturn(ignite);
	}

	@Test
	public void testInitCache() {
		PerformanceMonitor mon = new PerformanceMonitor(am);
		System.setProperty("series.capacity", "1000");
		mon.initIgniteCache();
		assertEquals(ignite, mon.getIgnite());
		assertEquals(1000, mon.getSeriesSize());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test1SeriesCreate() throws Exception {
		PerformanceMonitor mon = new PerformanceMonitor(am);
		mon.initIgniteCache();
		Event event = new SimpleEvent();
		Gson gson = new Gson();
		JsonObject obj = new JsonObject();
		obj.addProperty("name", "mcm.rule.efficiency.10");
		obj.addProperty("seriesName", "mcm.rule.efficiency");
		obj.addProperty("ruleId", "10");
		obj.addProperty("rule_group", "test");
		obj.addProperty("value", (Number) (10));
		event.getHeaders().put("timestamp", String.valueOf(System.currentTimeMillis()));
		event.setBody(gson.toJson(obj).getBytes());
		final int limit = 7;
		int counter = 0;
		for (int i = 0; i < limit; i++) {
			mon.processEvent(event);
		}
		for (Entry<Object, Object> entry : ignite.cache("mcmMetrics")) {
			System.err.println("\tMCM metric:" + entry.getKey() + "\t" + entry.getValue());
			Set<String> set = ((Set<String>) entry.getValue());
			for (String series : set) {
				System.err.println("\t\tSeries names:" + series);
				IgniteSet<String> set2 = ignite.set(series, mon.getCfg());
				for (String string : set2) {
					IgniteQueue<Object> queue = ignite.queue(string, 100, mon.getColCfg());
					for (Object object : queue) {
						System.err.println("\t\t\tSeries values:" + object);
						counter++;
					}
				}
			}
		}
		assertEquals(limit, counter);
	}

	@Test
	public void test2SeriesGet() throws Exception {
		PerformanceMonitor mon = new PerformanceMonitor(am);
		mon.initIgniteCache();
		Event event = new SimpleEvent();
		Gson gson = new Gson();
		JsonObject obj = new JsonObject();
		obj.addProperty("name", "mcm.rule.efficiency.10");
		obj.addProperty("seriesName", "mcm.rule.efficiency");
		obj.addProperty("ruleId", "11");
		obj.addProperty("rule_group", "test");
		obj.addProperty("value", (Number) (10));
		event.getHeaders().put("timestamp", String.valueOf(System.currentTimeMillis()));
		event.setBody(gson.toJson(obj).getBytes());
		final int limit = 7;
		for (int i = 0; i < limit; i++) {
			mon.processEvent(event);
		}
		Map<String, List<Point>> result = mon.getSeriesForRuleGroup("mcm.rule.efficiency", "test", 100);
		assertEquals(limit, result.get("11").size());
	}
}
