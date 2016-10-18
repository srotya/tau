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
package com.srotya.tau.wraith.aggregations;

import java.io.IOException;
import java.util.Map;

/**
 * Interface to define an Aggregation Engine
 * 
 * @author ambud_sharma
 */
public interface AggregationEngine {

	/**
	 * Initialize the aggregation engine
	 * 
	 * @param conf
	 * @param taskId
	 * @throws Exception
	 */
	public void initialize(Map<String, String> conf, int taskId) throws Exception;

	/**
	 * Flush results to the external state store
	 * 
	 * @throws IOException
	 */
	public void flush() throws IOException;

	/**
	 * Gracefully shutdown the engine
	 * 
	 * @throws IOException
	 */
	public void cleanup() throws IOException;

	/**
	 * Restore state from the data store
	 * 
	 * @throws IOException
	 */
	public void restore() throws IOException;

}