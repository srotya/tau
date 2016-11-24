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
import java.util.Map;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 * Coarse grain counting of unique items based on HyperLogLog (plus)
 * probabilistic data structure.
 * 
 * Use of Coarse counting should be used for counts above 10K to millions with
 * about 98-995 accuracy.
 * 
 * @author ambud_sharma
 */
public class CoarseCountingAggregator implements CountingAggregator {

	private static final boolean DISABLE_HARD_LIMIT_CHECKS = true;
	private static final long serialVersionUID = 1L;
	private static final int HLL_PRECISION = 10;
	private HyperLogLogPlus hll;

	public CoarseCountingAggregator() {
		hll = new HyperLogLogPlus(HLL_PRECISION);
	}

	@Override
	public void initialize(Map<String, String> conf) {
	}

	@Override
	public int getHardLimit() {
		// pretty much no limit
		return Integer.MAX_VALUE;
	}

	@Override
	public CountingAggregator getInstance() {
		return new CoarseCountingAggregator();
	}

	@Override
	public long size() {
		return hll.cardinality();
	}

	@Override
	public boolean add(Object aggregationValue) {
		return hll.offer((Integer) aggregationValue);
	}

	@Override
	public boolean disableLimitChecks() {
		return DISABLE_HARD_LIMIT_CHECKS;
	}

	@Override
	public Object getDatastructure() {
		return hll;
	}

	@Override
	public void reset() {
		hll = new HyperLogLogPlus(HLL_PRECISION);
	}

	@Override
	public double getCardinality() {
		return size();
	}

	@Override
	public void initialize(Object data) throws IOException {
		if (data instanceof byte[]) {
			hll = HyperLogLogPlus.Builder.build((byte[]) data);
		}
	}

}
