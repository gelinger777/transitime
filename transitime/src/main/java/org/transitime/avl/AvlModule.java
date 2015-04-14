/* 
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transitime.avl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitime.configData.AvlConfig;
import org.transitime.db.structs.AvlReport;
import org.transitime.ipc.jms.JMSWrapper;
import org.transitime.ipc.jms.RestartableMessageProducer;
import org.transitime.modules.Module;
import org.transitime.utils.threading.BoundedExecutor;
import org.transitime.utils.threading.NamedThreadFactory;


/**
 * Low-level abstract AVL module class that handles the processing
 * of the data. Uses JMS to queue the data if JMS enabled.
 * 
 * @author SkiBu Smith
 * 
 */
public abstract class AvlModule extends Module {
	// For writing the AVL data to the JMS topic
	protected RestartableMessageProducer jmsMsgProducer = null; 

	// For when using a direct queue instead of JMS for processing the AVL 
	// reports
	private final static int MAX_THREADS = 100;
	private BoundedExecutor avlClientExecutor = null;

	private static final Logger logger = 
			LoggerFactory.getLogger(AvlModule.class);	

	/********************** Member Functions **************************/

	/**
	 * Constructor
	 * 
	 * @param agencyId
	 */
	protected AvlModule(String agencyId) {
		super(agencyId);		
		
		if (!AvlConfig.shouldUseJms()) 
			initForUsingQueueInsteadOfJms();
	}
	
	/**
	 * For when using a direct queue instead of JMS for handling
	 * the AVL reports.
	 */
	private void initForUsingQueueInsteadOfJms() {
		int maxAVLQueueSize = AvlConfig.getAvlQueueSize();
		int numberThreads = AvlConfig.getNumAvlThreads();
		
		logger.info("Starting AvlModule for directly handling AVL reports " +
				"via a queue instead of JMS. For agencyId={} with "
				+ "maxAVLQueueSize={} and numberThreads={}", agencyId,
				maxAVLQueueSize, numberThreads);

		// Make sure that numberThreads is reasonable
		if (numberThreads < 1) {
			logger.error("Number of threads must be at least 1 but {} was "
					+ "specified. Therefore using 1 thread.", numberThreads);
			numberThreads = 1;
		}
		if (numberThreads > MAX_THREADS) {
			logger.error("Number of threads must be no greater than {} but "
					+ "{} was specified. Therefore using {} threads.",
					MAX_THREADS, numberThreads, MAX_THREADS);
			numberThreads = MAX_THREADS;
		}

		// Create the executor that actually processes the AVL data. The executor
		// will be passed an AvlClient and then AvlClient.run() is called.
		NamedThreadFactory avlClientThreadFactory = 
				new NamedThreadFactory("avlClient"); 
		Executor executor = Executors.newFixedThreadPool(numberThreads,
				avlClientThreadFactory);
		avlClientExecutor = new BoundedExecutor(executor, maxAVLQueueSize);
	}
	
	/**
	 * Initializes JMS if need be. Needs to be done from same thread that
	 * JMS is written to. Otherwise get concurrency error. 
	 */
	private void initializeJmsIfNeedTo() {
		// If JMS already initialized then can return
		if (jmsMsgProducer != null)
			return;
		
		// JMS not already initialized so create the MessageProducer 
		// that the AVL data can be written to
		try {
			String jmsTopicName = AvlJmsClientModule.getTopicName(agencyId);
			JMSWrapper jmsWrapper = JMSWrapper.getJMSWrapper();
			jmsMsgProducer = jmsWrapper.createTopicProducer(jmsTopicName);
		} catch (Exception e) {
			logger.error("Problem when setting up JMSWrapper for the AVL feed", e);			
		}

	}
	
	/**
	 * Processes AVL report read from feed. To be called from the subclass for
	 * each AVL report. Can use JMS or bypass it, depending on how configured.
	 */
	protected void processAvlReport(AvlReport avlReport) {
		if (AvlConfig.shouldUseJms()) {
			processAvlReportUsingJms(avlReport);
		} else {
			processAvlReportWithoutJms(avlReport);
		}
	}
	
	/**
	 * Sends the AvlReport object to the JMS topic so that AVL clients can read it.
	 * @param avlReport
	 */
	private void processAvlReportUsingJms(AvlReport avlReport) {
		// Make sure the JMS stuff setup successfully
		initializeJmsIfNeedTo();
		if (jmsMsgProducer == null) {
			logger.error("Cannot write AvlReport to JMS because JMS tools " + 
					"were not initialized successfully.");
			return;
		}
			
		// Send the AVL report to the JMS topic
		try {
			jmsMsgProducer.sendObjectMessage(avlReport);
		} catch (JMSException e) {
			logger.error("Problem sending AvlReport to the JMS topic", e);
		}		
	}

	/**
	 * Instead of writing AVL report to JMS topic this method directly
	 * processes it. By doing this one can bypass the need for a JMS server.
	 * 
	 * @param avlReport
	 */
	private void processAvlReportWithoutJms(AvlReport avlReport) {
		// Have another thread actually process the AVL data
		// using the AvlClient class. Actually uses AvlClient.run() to
		// do the processing. This way can use multiple
		// threads to simultaneously process the data.
		Runnable avlClient = new AvlClient(avlReport);
		try {
			avlClientExecutor.execute(avlClient);
		} catch (InterruptedException e) {
			logger.error("Exception when processing AVL data", e);
		}								
	}
}