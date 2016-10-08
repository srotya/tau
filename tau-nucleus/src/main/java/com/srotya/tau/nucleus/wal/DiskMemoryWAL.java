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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.EventFactory;

/**
 * @author ambudsharma
 *
 */
public class DiskMemoryWAL implements WAL {
	
	static int length = 1024*1024*1024;//;0x8FFFFFF; // 128 Mb
	private SortedMap<String, Long> offsetMap;
	private String walDirectory;
	private MappedByteBuffer wal;
	private String transientDirectory;

	public DiskMemoryWAL() {
		offsetMap = new ConcurrentSkipListMap<>();
	}

	@Override
	public void start() throws Exception {
		wal = new RandomAccessFile(new File(walDirectory+"/wal.bin"), "rw").getChannel().map(MapMode.READ_WRITE, 0, length);
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	public synchronized void writeEvent(String eventId, byte[] event) throws IOException {
		offsetMap.put(eventId, (long)wal.position());
		byte[] id = eventId.getBytes();
//		wal.writeInt(id.length);
//		wal.write(id);
//		wal.writeInt(event.length);
//		wal.write(event);
		wal.putInt(id.length);
		wal.put(id);
		wal.putInt(event.length);
		wal.put(event);
	}

	@Override
	public void ackEvent(String eventId) throws IOException {
		offsetMap.remove(eventId);
	}

	@Override
	public String getEarliestEventId() throws IOException {
		return offsetMap.firstKey();
	}

	@Override
	public void setPersistentDirectory(String persistentDirectory) {
		walDirectory = persistentDirectory;
	}

	@Override
	public void setTransientDirectory(String transientDirectory) {
		this.transientDirectory = transientDirectory;
	}

	@Override
	public void setCallingProcessor(AbstractProcessor processor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEventFactory(EventFactory factory) {
		// TODO Auto-generated method stub
		
	}

}
