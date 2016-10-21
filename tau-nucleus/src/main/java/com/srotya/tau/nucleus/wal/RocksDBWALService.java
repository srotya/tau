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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.rocksdb.util.SizeUnit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.nucleus.Utils;
import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;

/**
 * {@link WAL} implementation based on Facebook's {@link RocksDB} which is
 * sorted key value store inspired by HBase. <br>
 * <br>
 * It offers high performance features with persistence making it really good
 * for a {@link WAL} use case as it's optimized for flash storage thus keeping
 * the primary tree in memory (RAMFS) and {@link RocksDB}'s Write-Ahead-Log on
 * disk. <br>
 * <br>
 * Therefore faults can be reliably tolerated while achieving fairly high
 * performance. This is better than Kafka for the Nucleus inter-processor queue
 * use case since individual events can be acknowledged without loosing track of
 * the earliest unacknowledged event.
 * 
 * @author ambudsharma
 */
public class RocksDBWALService implements WAL {

	private static final Logger logger = Logger.getLogger(RocksDBWALService.class.getName());
	private static final Charset charset = Charset.forName("utf-8");
	private RocksDB wal;
	private Options options;
	private String walDirectory;
	private boolean autoResetWal = false;
	private String mapDirectory;
	private AbstractProcessor processor;
	private EventFactory factory;
	private Gson gson;
	private Type type;
	private WriteOptions opts;

	static {
		RocksDB.loadLibrary();
	}

	public RocksDBWALService() {
		gson = new Gson();
		type = new TypeToken<HashMap<String, Object>>() {
		}.getType();
	}

	@SuppressWarnings("resource")
	@Override
	public void start() throws Exception {
		if (autoResetWal) {
			Utils.wipeDirectory(walDirectory);
			Utils.wipeDirectory(mapDirectory);
			logger.info("Cleared WAL directory:" + walDirectory);
		}
		options = new Options().setCreateIfMissing(true).setAllowMmapReads(true).setAllowMmapWrites(true)
				.setIncreaseParallelism(2).setFilterDeletes(true).setMaxBackgroundCompactions(10)
				.setMaxBackgroundFlushes(10).setDisableDataSync(false).setUseFsync(false).setUseAdaptiveMutex(false)
				.setWriteBufferSize(1 * SizeUnit.MB).setCompactionStyle(CompactionStyle.UNIVERSAL)
				.setCompressionType(CompressionType.SNAPPY_COMPRESSION).setMaxWriteBufferNumber(6).setWalTtlSeconds(60)
				.setWalSizeLimitMB(512).setMaxTotalWalSize(1024 * SizeUnit.MB).setErrorIfExists(false)
				.setAllowOsBuffer(true).setWalDir(walDirectory).setOptimizeFiltersForHits(false);
		wal = RocksDB.open(options, mapDirectory);
		opts = new WriteOptions().setDisableWAL(false).setSync(false);
		recoverAndReplayEvents();
	}

	public void recoverAndReplayEvents() {
		RocksIterator itr = wal.newIterator();
		try {
			itr.seekToFirst();
			while (itr.isValid() && itr.key() != null) {
				String id = new String(itr.key(), charset);
				String body = new String(itr.value(), charset);
				logger.fine("Recovered non-acked event, eventid:" + id + "\tbody:" + body);
				Event event = factory.buildEvent();
				event.setEventId(id);
				event.getHeaders().putAll(gson.fromJson(body, type));
				event.setBody(itr.value());
				processor.processEventNonWaled(event);
				itr.next();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to recover events", e);
		} finally {
			itr.close();
		}
	}

	@Override
	public void stop() throws Exception {
		wal.close();
		opts.close();
		options.close();
	}

	public void writeEvent(String eventId, byte[] event) throws IOException {
		try {
			wal.put(eventId.getBytes(charset), event);
		} catch (RocksDBException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @return the parserWal
	 */
	protected RocksDB getParserWal() {
		return wal;
	}

	@Override
	public void ackEvent(String eventId) throws IOException {
		try {
			wal.remove(opts, eventId.getBytes(charset));
		} catch (RocksDBException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getEarliestEventId() throws IOException {
		RocksIterator itr = wal.newIterator();
		itr.seekToFirst();
		if(!itr.isValid()) {
			return null;
		}
		String val = new String(itr.key(), charset);
		itr.close();
		return val;
	}

	@Override
	public void setPersistentDirectory(String persistentDirectory) {
		walDirectory = persistentDirectory;
	}

	@Override
	public void setTransientDirectory(String transientDirectory) {
		mapDirectory = transientDirectory;
	}

	@Override
	public void setCallingProcessor(AbstractProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void setEventFactory(EventFactory factory) {
		this.factory = factory;
	}

	/**
	 * For simple rocksdb testing
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		RocksDBWALService wal = new RocksDBWALService();
		wal.setPersistentDirectory("target/wal");
		wal.setTransientDirectory("target/mem");
		wal.start();
		wal.writeEvent("event1", "{ \"event\":\"header\"}".getBytes());
	}
}