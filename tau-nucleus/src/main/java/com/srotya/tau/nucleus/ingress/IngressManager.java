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
package com.srotya.tau.nucleus.ingress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.srotya.tau.nucleus.BGTaskManager;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.metrics.MetricsSink;
import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.Event;

import io.dropwizard.lifecycle.Managed;

/**
 * @author ambudsharma
 */
public class IngressManager implements Managed {

	public static final String METRIC_INGRESS_EPS = "ingress.eps";
	public static final String METRIC_INGRESS_LIFETIME = "ingress.eps.lifetime";
	private static final Logger logger = Logger.getLogger(IngressManager.class.getName());
	protected static final String METRIC_INGRESS_BPS = "ingress.bps";
	protected static final String METRIC_INGRESS_BPS_LIFETIME = "ingress.bps.lifetime";
	private ExecutorService pool;
	private IngresserFactory factory;
	private List<IngressRunner> ingressList = new ArrayList<>();
	private AtomicBoolean terminationControl;
	private AbstractProcessor nextProcessor;

	/**
	 * @param nextProcessor
	 * @param factory
	 * @param wal
	 */
	public IngressManager(AbstractProcessor nextProcessor, IngresserFactory factory) {
		this.nextProcessor = nextProcessor;
		this.factory = factory;
		this.terminationControl = new AtomicBoolean(true);
	}

	@Override
	public final void start() throws Exception {
		if (nextProcessor.getDisruptorBuffer() == null) {
			throw new RuntimeException("Next processor for IngressManager is null");
		}
		ingressRateMonitor();
		factory.initialize();
		pool = Executors.newFixedThreadPool(factory.getIngresserParallelism());
		for (int i = 0; i < factory.getIngresserParallelism(); i++) {
			IngressRunner ingressRunner = new IngressRunner(factory.buildIngresser(i), nextProcessor,
					terminationControl);
			ingressList.add(ingressRunner);
			pool.submit(ingressRunner);
		}
	}

	/**
	 * Monitors the ingress rate across all {@link IngressRunner}s of this
	 * Nucleus instance
	 */
	protected void ingressRateMonitor() {
		BGTaskManager.getInstance().schedule(new Runnable() {

			private long counter = 0;
			private long bps = 0;

			@Override
			public void run() {
				long temp = 0, tbps = 0;
				for (IngressRunner runner : ingressList) {
					temp += runner.getEps();
					tbps += runner.getBps();
				}
				long eps = (temp - counter);
				long rbps = (tbps - bps);
				MetricsSink.getInstance().publishIntMetric(METRIC_INGRESS_EPS, eps);
				logger.info("Ingress EPS:" + eps + "\tLifetime:" + temp + "\tBPS:" + (rbps / (1024 * 1024)) + " MB/s");
				MetricsSink.getInstance().publishIntMetric(METRIC_INGRESS_LIFETIME, temp);
				MetricsSink.getInstance().publishIntMetric(METRIC_INGRESS_BPS, rbps);
				MetricsSink.getInstance().publishIntMetric(METRIC_INGRESS_BPS_LIFETIME, bps);
				counter = temp;
				bps = tbps;
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	@Override
	public final void stop() throws Exception {
		terminationControl.set(false);
		pool.shutdownNow();
		pool.awaitTermination(2, TimeUnit.SECONDS);
	}

	/**
	 * @author ambudsharma
	 */
	public static class IngressRunner implements Runnable {

		public static final String FIELD_IG = "_ig";
		private PullIngresser ingresser;
		private long eps;
		private long bps;
		private AtomicBoolean ctrl;
		private AbstractProcessor nextProcessor;

		/**
		 * @param instance
		 * @param outputBuffer
		 * @param ctrl
		 */
		public IngressRunner(PullIngresser instance, AbstractProcessor nextProcessor, AtomicBoolean ctrl) {
			this.ingresser = instance;
			this.nextProcessor = nextProcessor;
			this.ctrl = ctrl;
		}

		@Override
		public void run() {
			while (ctrl.get()) {
				try {
					Event event = ingresser.produce();
					if (event != null && event.getEventId() != null) {
						try {
							this.nextProcessor.processEventWaled(event);
							eps++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}

		/**
		 * @return
		 */
		public long getEps() {
			return eps;
		}

		/**
		 * @return
		 */
		public long getBps() {
			return bps;
		}

	}

	/**
	 * @author ambudsharma
	 *
	 */
	public static interface IngresserFactory {

		/**
		 * Initialize the factory
		 * 
		 * @throws Exception
		 */
		public void initialize() throws Exception;

		/**
		 * Build ingresser with the task id
		 * 
		 * @param taskId
		 * @return
		 * @throws IOException
		 */
		public PullIngresser buildIngresser(int taskId) throws IOException;

		/**
		 * @return the conf
		 */
		public Properties getConf();

		/**
		 * @param conf
		 *            the conf to set
		 */
		public void setConf(Properties conf);

		/**
		 * @param ingresserParallelism
		 *            the ingresserParallelism to set
		 */
		public void setIngresserParallelism(int ingresserParallelism);

		/**
		 * @return ingresserParallelism
		 */
		public int getIngresserParallelism();

	}

	/**
	 * @author ambudsharma
	 *
	 */
	public static interface PullIngresser {

		/**
		 * @return
		 * @throws IOException
		 */
		public Event produce() throws IOException;

		/**
		 * @param event
		 */
		public void ack(Event event);

	}

	public static abstract class PushIngresser {

		private AbstractProcessor processor;
		private DisruptorUnifiedFactory factory;

		public PushIngresser(DisruptorUnifiedFactory factory, AbstractProcessor processor) {
			this.processor = processor;
			this.factory = factory;
		}

		/**
		 * @return the processor
		 */
		public AbstractProcessor getProcessor() {
			return processor;
		}

		/**
		 * @return the factory
		 */
		public DisruptorUnifiedFactory getFactory() {
			return factory;
		}

	}

}
