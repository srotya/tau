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
package com.srotya.tau.nucleus.wal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author ambudsharma
 *
 */
public class DiskMemoryWAL implements WAL {
	
	private SortedMap<String, Long> offsetMap;
	private String walDirectory;
	private RandomAccessFile wal;

	public DiskMemoryWAL(String walDirectory, String walMapDirectory) {
		this.walDirectory = walDirectory;
		offsetMap = new ConcurrentSkipListMap<>();
	}

	@Override
	public void start() throws Exception {
		wal = new RandomAccessFile(new File(walDirectory+"/wal.bin"), "rw");
	}

	@Override
	public void stop() throws Exception {
		wal.close();
	}

	@Override
	public synchronized void writeEvent(String eventId, byte[] event) throws IOException {
		offsetMap.put(eventId, wal.getFilePointer());
		byte[] id = eventId.getBytes();
		wal.writeInt(id.length);
		wal.write(id);
		wal.writeInt(event.length);
		wal.write(event);
	}

	@Override
	public synchronized void ackEvent(String eventId) throws IOException {
		offsetMap.remove(eventId);
	}

	@Override
	public String getEarliestEventId() throws IOException {
		return offsetMap.firstKey();
	}

}
