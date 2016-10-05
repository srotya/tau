package com.srotya.tau.wraith;

import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;

import org.junit.Test;

import com.srotya.tau.wraith.Utils;

public class TestUtils {

	@Test
	public void testRuleActionIdCombination() {
		for (int i = 0; i < 100; i++) {
			short ruleId = (short) (12 + i);
			short actionId = (short) (500 + i);
			String val = Utils.combineRuleActionId(ruleId, actionId);
			Entry<Short, Short> kv = Utils.separateRuleActionId(val);
			assertEquals(ruleId, (short) kv.getKey());
			assertEquals("Loop:" + i, actionId, (short) kv.getValue());
		}
	}

	@Test
	public void testRuleActionCombination() {
		for (int i = 0; i < 100; i++) {
			short ruleId = (short) (12 + i);
			short actionId = (short) (500 + i);
			byte[] val = Utils.combineRuleAction(ruleId, actionId);
			Entry<Short, Short> kv = Utils.separateRuleAction(val);
			assertEquals(ruleId, (short) kv.getKey());
			assertEquals("Loop:" + i, actionId, (short) kv.getValue());
		}
	}

	@Test
	public void testShortToByteConversion() {
		for (int i = 0; i < 1000; i++) {
			short val = (short) (553 + i);
			byte[] res = Utils.shortToBytes(val);
			short val2 = Utils.bytesToShort(res);
			assertEquals(val, val2);
		}
	}

	@Test
	public void testIntToStringEncoding() {
		for (int i = 0; i < 300; i++) {
			int number = (int)(System.currentTimeMillis()/1000) + i * 2;
			String val = Utils.intToString(number);
			assertEquals(number, Utils.stringToInt(val));
		}
	}

}
