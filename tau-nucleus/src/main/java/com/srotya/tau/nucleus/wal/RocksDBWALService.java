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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;

import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.util.SizeUnit;

/**
 * {@link WAL} implementation based on Facebook's {@link RocksDB} which is sorted key value store inspired by HBase.
 * <br><br>
 * It offers high performance features with persistence making it really good for a {@link WAL} use case as 
 * it's optimized for flash storage thus keeping the primary tree in memory (RAMFS) and {@link RocksDB}'s Write-Ahead-Log on disk.
 * <br><br>
 * Therefore faults can be reliably tolerated while achieving fairly high performance. This is better than Kafka for the Nucleus inter-processor 
 * queue use case since individual events can be acknowledged without loosing track of the earliest unacknowledged event. 
 * 
 * @author ambudsharma
 */
public class RocksDBWALService implements WAL {

	private static final Logger logger = Logger.getLogger(RocksDBWALService.class.getName());
	private static final Charset charset = Charset.forName("utf-8");
	private RocksDB wal;
	private String walDirectory;
	private Options options;
	private boolean autoResetWal = true;
	private String walMapDirectory;

	static {
		RocksDB.loadLibrary();
	}

	public RocksDBWALService(String walDirectory, String walMapDirectory) {
		this.walDirectory = walDirectory;
		this.walMapDirectory = walMapDirectory;
	}

	@SuppressWarnings("resource")
	@Override
	public void start() throws Exception {
		if (autoResetWal) {
			wipeDirectory(walDirectory);
			wipeDirectory(walMapDirectory);
			logger.info("Cleared WAL directory:" + walDirectory);
		}
		options = new Options().setCreateIfMissing(true).setAllowMmapReads(true).setAllowMmapWrites(true)
				.setIncreaseParallelism(2).setFilterDeletes(true).setMaxBackgroundCompactions(10)
				.setMaxBackgroundFlushes(10).setUseFsync(false).setUseAdaptiveMutex(false)
				.setWriteBufferSize(1 * SizeUnit.MB).setCompactionStyle(CompactionStyle.UNIVERSAL)
				.setCompressionType(CompressionType.SNAPPY_COMPRESSION).setMaxWriteBufferNumber(6).setWalTtlSeconds(60)
				.setWalSizeLimitMB(512).setMaxTotalWalSize(1024 * SizeUnit.MB).setErrorIfExists(true)
				.setAllowOsBuffer(true).setWalDir(walDirectory).setOptimizeFiltersForHits(false);
		wal = RocksDB.open(options, walMapDirectory);
	}

	private void wipeDirectory(String directory) {
		File file = new File(directory);
		if (file.isDirectory() && file.exists()) {
			Arrays.asList(file.listFiles()).forEach((f) -> {
				f.delete();
			});
			file.delete();
			file.mkdirs();
		}
	}

	@Override
	public void stop() throws Exception {
		wal.close();
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
			wal.remove(eventId.getBytes(charset));
		} catch (RocksDBException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getEarliestEventId() throws IOException {
		RocksIterator itr = wal.newIterator();
		itr.seekToFirst();
		return new String(itr.key(), charset);
	}

}