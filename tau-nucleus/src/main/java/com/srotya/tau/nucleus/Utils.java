/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.nucleus;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author ambudsharma
 *
 */
public class Utils {
	
	private Utils() {
	}
	
	public static Map<String, String> hashTableTohashMap(Hashtable<Object, Object> table) {
		Map<String, String> map = new HashMap<>();
		for (Entry<Object, Object> entry : table.entrySet()) {
			map.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return map;
	}
	
	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(x);
	    return buffer.array();
	}
	
	public static long byteToLong(byte[] x) {
	    ByteBuffer buffer = ByteBuffer.wrap(x);
	    return buffer.getLong();
	}

	public static void wipeDirectory(String directory) {
		File file = new File(directory);
		if (file.isDirectory() && file.exists()) {
			Arrays.asList(file.listFiles()).forEach((f) -> {
				f.delete();
			});
			file.delete();
		}
	}
}
