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
package com.srotya.tau.wraith.aggregators;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;

/**
 * Set aggregation provides aggregation capabilities for collecting actual data
 * values of a given {@link Event} value.
 * 
 * Sets are used to eliminate duplicates as well as be efficient about not
 * storing redundant information.
 * 
 * @author ambud_sharma
 */
public class SetAggregator implements Aggregator {

	private static final boolean DISABLE_HARD_LIMIT_CHECKS = false;
	private static final long serialVersionUID = 1L;
	private int hardLimit;
	private Set<Object> set;

	public SetAggregator() {
	}

	private SetAggregator(int hardLimit) {
		this.hardLimit = hardLimit;
		initialize();
	}

	protected void initialize() {
//		this.set = HashObjSets.newMutableSet((int) (hardLimit * Constants.SET_CAPACITY_AMPLIFICATION));
		set = new HashSet<>((int) (hardLimit * Constants.SET_CAPACITY_AMPLIFICATION), Constants.HASHSET_LOAD_FACTOR);
	}

	@Override
	public void initialize(Map<String, String> conf) {
		hardLimit = Integer.parseInt(conf
				.getOrDefault(Constants.AGGREGATIONS_SET_LIMIT, Constants.DEFAULT_AGGREGATION_SET_LIMIT).toString());
		initialize();
	}

	@Override
	public int getHardLimit() {
		return hardLimit;
	}

	@Override
	public Aggregator getInstance() {
		return new SetAggregator(hardLimit);
	}

	@Override
	public long size() {
		return set.size();
	}

	@Override
	public boolean add(Long timestamp, Object aggregationValue) {
		return set.add(aggregationValue);
	}

	@Override
	public boolean disableLimitChecks() {
		return DISABLE_HARD_LIMIT_CHECKS;
	}

	@Override
	public Object getDatastructure() {
		return set;
	}

	@Override
	public void reset() {
		set.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Object data) throws IOException {
		if(data instanceof Set) {
			set.addAll((Set<Object>)data);
		}
	}

}
