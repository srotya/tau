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

/**
 * To count exact values for a given quantity. The maximum hard limit of number
 * of accurately counted values is 100K to guarantee predictable performance.
 * This aggregator does counting based on a hashset and pre-hashes inbound
 * values to integers using MD5 thus ensuring predictable size.<br>
 * <br>
 * 
 * Each instance can / will store up to 400K bytes therefore allowing a decent
 * count number of accurate aggregations to be run.<br>
 * <br>
 * 
 * For quantities greater than 100K, you will need to use
 * {@link CoarseCountingAggregator} which utilizes HyperLogLog statistical data
 * structure for cardinality analysis.
 * 
 * Limits are enforced by dropping data.
 * 
 * @author ambud_sharma
 */
public class FineCountingAggregator implements CountingAggregator {

	private static final boolean DISABLE_HARD_LIMIT_CHECKS = false;
	private static final long serialVersionUID = 1L;
	private int hardLimit;
	private Set<Integer> set;

	public FineCountingAggregator() {
	}

	private FineCountingAggregator(int hardLimit) {
		this.hardLimit = hardLimit;
		initialize();
	}

	@Override
	public void initialize(Map<String, String> conf) {
		hardLimit = Integer.parseInt(
				conf.getOrDefault(Constants.AGGREGATIONS_FCOUNT_LIMIT, Constants.DEFAULT_AGGREGATION_FCOUNT_LIMIT)
						.toString());
		initialize();
	}

	protected void initialize() {
		set = new HashSet<>((int) (hardLimit * Constants.SET_CAPACITY_AMPLIFICATION), Constants.HASHSET_LOAD_FACTOR);
	}

	@Override
	public int getHardLimit() {
		return hardLimit;
	}

	@Override
	public CountingAggregator getInstance() {
		return new FineCountingAggregator(hardLimit);
	}

	@Override
	public long size() {
		return set.size();
	}

	@Override
	public boolean add(Object aggregationValue) {
		return set.add((Integer) aggregationValue);
	}

	@Override
	public boolean disableLimitChecks() {
		return DISABLE_HARD_LIMIT_CHECKS;
	}

	@Override
	public Set<Integer> getDatastructure() {
		return set;
	}

	@Override
	public void reset() {
		set.clear();
	}

	@Override
	public long getCardinality() {
		return size();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Object data) throws IOException {
		if (data instanceof Set) {
			set.addAll((Set<Integer>) data);
		}
	}

}
