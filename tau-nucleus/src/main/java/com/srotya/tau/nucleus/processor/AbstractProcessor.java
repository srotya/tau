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
package com.srotya.tau.nucleus.processor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.ManagedProcessor;
import com.srotya.tau.nucleus.disruptor.CopyTranslator;
import com.srotya.tau.nucleus.ingress.IngressManager.PullIngresser;
import com.srotya.tau.nucleus.wal.WAL;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;

/**
 * An abstract processor is responsible for internalizing the concept of
 * guaranteed at least once event processing for Tau's components. Each
 * {@link AbstractProcessor} contains an in-memory {@link RingBuffer} and a
 * persistent {@link WAL} (Write-ahead-log) <br>
 * <br>
 * An {@link Event} is only safe from faults when it has been successfully been
 * written to both the {@link RingBuffer} and the {@link WAL}. This
 * functionality is exposed via the processEvent method that synchronously
 * writes to both buffer and wal and only then returns the call. <br>
 * <br>
 * An exception may be thrown if there's a fault while writing to the
 * {@link WAL} or the {@link RingBuffer} which is to be handled by the caller.
 * <br>
 * <br>
 * Callers can be an {@link PullIngresser} or other {@link AbstractProcessor}
 * which is why the constructor requires an array of forward
 * {@link AbstractProcessor} which makes allows for the processing DAG to be
 * constructed (allowing loops). <br>
 * <br>
 * Each {@link AbstractProcessor} is independently parallelized and therefore
 * can be independently scale as necessary given that resources are available.
 * 
 * @author ambudsharma
 */
public abstract class AbstractProcessor implements ManagedProcessor {

	private WAL selfWal;
	private AbstractProcessor[] outputProcessors;
	private Disruptor<Event> disruptor;
	private ExecutorService pool;
	private MutableInt parallelism;
	private DisruptorUnifiedFactory factory;
	private int bufferSize;
	private Map<String, String> conf;
	private RingBuffer<Event> buffer;
	private boolean started;
	private CopyTranslator copyTranslator;

	public AbstractProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize, Map<String, String> conf,
			AbstractProcessor... outputProcessors) {
		this.outputProcessors = outputProcessors;
		this.parallelism = new MutableInt();
		this.parallelism.setVal(parallelism);
		this.factory = factory;
		this.bufferSize = bufferSize;
		this.conf = conf;
		this.copyTranslator = new CopyTranslator();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public final void start() throws Exception {
		if (getConfigPrefix() != null) {
			selfWal = factory.newWalInstance(conf.get(getConfigPrefix() + ".wal.wdir"),
					conf.get(getConfigPrefix() + ".wal.mdir"));
			selfWal.start();
		}else {
			getLogger().warning("WAL is disabled");
		}
		pool = Executors.newFixedThreadPool(parallelism.getVal());
		disruptor = new Disruptor<>(factory, bufferSize, pool, ProducerType.MULTI, new BlockingWaitStrategy());
		List<EventHandler<Event>> handlers = getInitializedHandlers(parallelism, conf, factory);
		disruptor.handleEventsWith(handlers.toArray(new EventHandler[1]));
		buffer = disruptor.start();
		started = true;
	}

	@Override
	public final void stop() throws Exception {
		pool.shutdownNow();
		pool.awaitTermination(1, TimeUnit.SECONDS);
		selfWal.stop();
		started = false;
	}

	public abstract List<EventHandler<Event>> getInitializedHandlers(MutableInt parallelism, Map<String, String> conf,
			DisruptorUnifiedFactory factory) throws Exception;

	/**
	 * @param event
	 * @throws IOException
	 */
	public final void processEventWaled(Event event) throws IOException {
		selfWal.writeEvent(event.getEventId(), event.getBody());
		buffer.publishEvent(copyTranslator, event);
	}

	/**
	 * @param event
	 * @throws IOException
	 */
	public final void processEventNonWaled(Event event) {
		buffer.publishEvent(copyTranslator, event);
	}

	/**
	 * @return the outputProcessors
	 */
	public final AbstractProcessor[] getOutputProcessors() {
		return outputProcessors;
	}

	/**
	 * @param outputProcessors
	 */
	public final void setOutputProcessors(AbstractProcessor... outputProcessors) {
		if (outputProcessors != null) {
			this.outputProcessors = outputProcessors;
		}
	}

	/**
	 * @return
	 */
	public final boolean isStarted() {
		return started;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public final void ackEvent(String eventId) throws IOException {
		selfWal.ackEvent(eventId);
	}

	@Override
	public final RingBuffer<Event> getDisruptorBuffer() {
		return buffer;
	}

	@Override
	public final WAL getProcessorWal() {
		return selfWal;
	}

	public abstract String getConfigPrefix();
	
	public abstract Logger getLogger();
}
