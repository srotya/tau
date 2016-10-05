/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.dengine.bolts;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.TupleUtils;
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;
import storm.kafka.bolt.selector.KafkaTopicSelector;


/**
 * This bolt is a backport of Storm 1.0.0 KafkaBolt to make it compatible with flux
 * 
 * Bolt implementation that can send Tuple data to Kafka
 * <br>
 * It expects the producer configuration and topic in storm config under
 * <br>
 * 'kafka.broker.properties' and 'topic'
 * <br>
 * respectively.
 * <br>
 * This bolt uses 0.8.2 Kafka Producer API.
 * <br>
 * It works for sending tuples to older Kafka version (0.8.1).
 */
public class KafkaBolt<K, V> extends BaseRichBolt {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(KafkaBolt.class);

    public static final String TOPIC = "topic";

    private KafkaProducer<K, V> producer;
    private OutputCollector collector;
    private TupleToKafkaMapper<K,V> mapper;
    private KafkaTopicSelector topicSelector;
    private Properties boltSpecfiedProperties = new Properties();
    /**
     * With default setting for fireAndForget and async, the callback is called when the sending succeeds.
     * By setting fireAndForget true, the send will not wait at all for kafka to ack.
     * "acks" setting in 0.8.2 Producer API config doesn't matter if fireAndForget is set.
     * By setting async false, synchronous sending is used. 
     */
    private boolean fireAndForget = false;
    private boolean async = true;

    public KafkaBolt() {}

    public KafkaBolt<K,V> withTupleToKafkaMapper(TupleToKafkaMapper<K,V> mapper) {
        this.mapper = mapper;
        return this;
    }

    public KafkaBolt<K,V> withTopicSelector(KafkaTopicSelector selector) {
        this.topicSelector = selector;
        return this;
    }

    public KafkaBolt<K,V> withProducerProperties(Properties producerProperties) {
        this.boltSpecfiedProperties = producerProperties;
        return this;
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        //for backward compatibility.
        if(mapper == null) {
            this.mapper = new FieldNameBasedTupleToKafkaMapper<K,V>();
        }

        //for backward compatibility.
        if(topicSelector == null) {
            this.topicSelector = new DefaultTopicSelector((String) stormConf.get(TOPIC));
        }

        producer = new KafkaProducer<>(boltSpecfiedProperties);
        this.collector = collector;
    }

    @Override
    public void execute(final Tuple input) {
        if (TupleUtils.isTick(input)) {
          collector.ack(input);
          return; // Do not try to send ticks to Kafka
        }
        K key = null;
        V message = null;
        String topic = null;
        try {
            key = mapper.getKeyFromTuple(input);
            message = mapper.getMessageFromTuple(input);
            topic = topicSelector.getTopic(input);
            if (topic != null ) {
                Callback callback = null;

                if (!fireAndForget && async) {
                    callback = new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata ignored, Exception e) {
                            synchronized (collector) {
                                if (e != null) {
                                    collector.reportError(e);
                                    collector.fail(input);
                                } else {
                                    collector.ack(input);
                                }
                            }
                        }
                    };
                }
                Future<RecordMetadata> result = producer.send(new ProducerRecord<K, V>(topic, key, message), callback);
                if (!async) {
                    try {
                        result.get();
                        collector.ack(input);
                    } catch (ExecutionException err) {
                        collector.reportError(err);
                        collector.fail(input);
                    }
                } else if (fireAndForget) {
                    collector.ack(input);
                }
            } else {
                LOG.warn("skipping key = " + key + ", topic selector returned null.");
                collector.ack(input);
            }
        } catch (Exception ex) {
            collector.reportError(ex);
            collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void cleanup() {
        producer.close();
    }

    public void setFireAndForget(boolean fireAndForget) {
        this.fireAndForget = fireAndForget;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}