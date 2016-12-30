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

import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;

import io.dropwizard.lifecycle.Managed;

/**
 * {@link WAL} or Write-Ahead-Log is the primary pillar of fault tolerance in
 * Nucleus. <br>
 * <br>
 * The WAL ideally must be backed by a persistent media in some way to guarantee
 * recovery from a hard-fault (machine reboot or power failure). <br>
 * <br>
 * WAL must also keep a checkpoint of what {@link Event} has and hasn't been
 * processed yet and thus implement methods to write events to the WAL and
 * acknowledge {@link Event}s in the WAL marking them as processed. <br>
 * <br>
 * The ackEvent function of the WAL must only be called by when the module is
 * certain that all processing has been successfully completed and results have
 * been written out to either another WAL or the processing tree has terminated.
 * This is required to guarantee processing of {@link Event}s and the
 * at-least-once semantics of the real-time stream processing world.
 * 
 * @author ambudsharma
 */
public interface WAL extends Managed {

	/**
	 * Write an event to the WAL
	 * 
	 * @param event
	 * @throws IOException
	 */
	public void writeEvent(Event event) throws IOException;

	/**
	 * Acknowledge an {@link Event} in the WAL and mark it processed so in case the system
	 * crashes or is stopped the event will not be replayed
	 * 
	 * @param eventId
	 * @throws IOException
	 */
	public void ackEvent(Long eventId) throws IOException;

	/**
	 * Get the ID of the earliest {@link Event} that is still in the {@link WAL}
	 * 
	 * @return
	 * @throws IOException
	 */
	public Long getEarliestEventId() throws IOException;

	/**
	 * {@link WAL} directory that is written to a persistent media
	 * 
	 * @param persistentDirectory
	 */
	public void setPersistentDirectory(String persistentDirectory);

	/**
	 * {@link WAL} directory that may be written to a transient media like tmpfs
	 * or ramfs
	 * 
	 * @param transientDirectory
	 */
	public void setTransientDirectory(String transientDirectory);

	/**
	 * This reference is set so that that WAL is able to replay data that wasn't
	 * yet processed after the last failure
	 * 
	 * @param processor
	 */
	public void setCallingProcessor(AbstractProcessor processor);

	/**
	 * To create events for recovery
	 * 
	 * @param factory
	 */
	public void setEventFactory(EventFactory factory);
}
