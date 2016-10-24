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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

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
	
	/**
	 * Build a {@link CloseableHttpClient}
	 * 
	 * @param baseURL
	 * @param connectTimeout
	 * @param requestTimeout
	 * @return http client
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 */
	public static CloseableHttpClient buildClient(String baseURL, int connectTimeout, int requestTimeout)
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		HttpClientBuilder clientBuilder = HttpClients.custom();
		RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
				.setConnectionRequestTimeout(requestTimeout).build();

		return clientBuilder.setDefaultRequestConfig(config).build();
	}

	/**
	 * Get client
	 * 
	 * @param baseURL
	 * @param connectTimeout
	 * @param requestTimeout
	 * @return client
	 */
	public static CloseableHttpClient getClient(String baseURL, int connectTimeout, int requestTimeout) {
		try {
			return buildClient(baseURL, connectTimeout, requestTimeout);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			return null;
		}
	}
}
