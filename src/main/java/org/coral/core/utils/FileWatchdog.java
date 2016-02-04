package org.coral.core.utils;


import java.io.File;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * This class monitors a specified File for a change and notifies back when the file on disk has changed.
 * It is based on the last modification time of the file.
 *  
 */
public final class FileWatchdog extends Observable {

	private static final AtomicLong SEQ = new AtomicLong(0);
	
	/**
	 * This is the file that is to be monitored
	 */
	File fileToWatch = null;

	/**
	 * Full path to the file
	 */
	String cannonicalFilePath = null;

	/**
	 * When the file was last modified
	 */
	long lastModifiedTime;

	/**
	 * Time between checks
	 */
	long TIME_TO_SLEEP;

	/**
	 * Manager instance
	 */
	static DFWManager dfwManager = null;

	/**
	 * Unique ID
	 */
	long seq;

	private static final Object lock = new Object();

	private static final class DFWEntry implements Delayed {
		/**
		 * When this entry will expire and the file will be checked for modifications
		 */
		long nextCheck;

		/**
		 * File watcher
		 */
		final FileWatchdog dfw;

		/**
		 * Unique ID
		 */
		final long seq;

		/**
		 * The only constructor
		 * 
		 * @param dfwMonitor file to watch
		 * @param lNextCheck when to check next time
		 */
		public DFWEntry(final FileWatchdog dfwMonitor, final long lNextCheck) {
			this.dfw = dfwMonitor;
			this.nextCheck = lNextCheck;
			this.seq = dfwMonitor.seq;
		}

		/**
		 * Implementation of the Delayed interface
		 * 
		 * @param o object to compare to, by time
		 * @return time sorting of the objects to expire
		 */
		public int compareTo(final Delayed o) {
			if (o.equals(this))
				return 0;

			final DFWEntry dfe = (DFWEntry) o;

			if (dfe.nextCheck > this.nextCheck)
				return -1;
			
			if (dfe.nextCheck < this.nextCheck)
				return 1;

			if (dfe.seq < this.seq)
				return 1;
			
			if (dfe.seq > this.seq)
				return -1;

			return 0;
		}

		/**
		 * Implementation of the Delayed interface
		 * 
		 * @param unit unit to return the results into
		 * @return how much time is left until the next check of this file
		 */
		public long getDelay(final TimeUnit unit) {
			return unit.convert(this.nextCheck - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}

		/**
		 * Check if two objects are equal
		 */
		@Override
		public boolean equals(final Object o) {
			if (o == null)
				return false;

			if (o == this)
				return true;

			if (o instanceof DFWEntry && ((DFWEntry) o).seq == this.seq)
				return true;//is it this possible to reach this ?!?

			return false;
		}

		/**
		 * Hash code by the sequence number
		 * 
		 * @return the hash code based on the sequence number of this watchdog
		 */
		@Override
		public int hashCode() {
			return (int) (this.seq ^ (this.seq >>> 32));
		}

	}

	private static final class DFWManager extends Thread {
		protected Log logger = LogFactory.getLog(getClass());
		/**
		 * Delay queue, used for easy take of the next entry that is about to expire and check it
		 */
		final DelayQueue<DFWEntry> queue = new DelayQueue<DFWEntry>();

		/**
		 * This structure holds all the file monitors
		 */
		final ConcurrentHashMap<Long, DFWEntry>cmap = new ConcurrentHashMap<Long, DFWEntry>();

		private boolean hasToRun;

		/**
		 * Set the thread title and make the thread daemon
		 */
		public DFWManager() {
			super("FileWatchdogManager");
			
			try {
				setDaemon(true);
			} 
			catch (Throwable t) {
				// well, we tried :)
			}
			
			this.hasToRun = true;
		}
		
		/**
		 * Notify the thread that it's time to exit
		 */
		public void signalStop(){
			this.hasToRun = false;
		}

		private void checkEntry(final DFWEntry dfe) {
			try {
				final FileWatchdog dfw = dfe.dfw;
				
				final long lmt = dfw.fileToWatch.lastModified();
				
				if (dfw.lastModifiedTime != lmt) {
					dfw.lastModifiedTime = lmt;
					dfw.setChanged();
					dfw.notifyObservers();
				}
			} 
			catch (Throwable t) {
				logger.warn("Got exception trying to monitor for a file change [ "+ dfe.dfw.cannonicalFilePath + " ]", t);
			}
		}

		/**
		 * Add a new file monitor to this global watcher
		 * 
		 * @param dfw
		 * @param nextCheck
		 */
		void add(final FileWatchdog dfw, final long nextCheck) {
			if (dfw == null)
				return;
			
			final DFWEntry dfe = new DFWEntry(dfw, nextCheck);
			this.queue.add(dfe);
			this.cmap.put(Long.valueOf(dfw.seq), dfe);
			
			setName("FileWatchdogManager [ " + this.queue.size() + " ]");
		}

		/**
		 * Stop watching a file
		 * 
		 * @param dfw
		 */
		void remove(final FileWatchdog dfw) {
			if (dfw == null)
				return;
			
			final DFWEntry dfe = this.cmap.remove(Long.valueOf(dfw.seq));
			
			if (dfe != null) {
				this.queue.remove(dfe);
				setName("FileWatchdogManager [ " + this.queue.size() + " files watched ]");
			}
		}

		/**
		 * Thread body, waits for an entry to expire then checks it
		 */
		@Override
		public void run() {
			logger.info("FileWatchdog started");
			
			while (this.hasToRun) {
				try {
					DFWEntry dfe = null;
					
					try {
						dfe = this.queue.take();
					}
					catch (InterruptedException ie) {
						// if we are interrupted the process will start again
					}
					
					if (dfe == null) {
						try {
							Thread.sleep(5 * 1000);
						} 
						catch (InterruptedException ie) {
							// If the extracted element was null it means that the queue has previously received an
							// interruption. We are sleeping for a while, and if we are interrupted again ... np :)
						}
					} 
					else {
						checkEntry(dfe);
						add(dfe.dfw, System.currentTimeMillis() + dfe.dfw.TIME_TO_SLEEP);
					}
				} 
				catch (Throwable t) {
					logger.warn("FileWatchdog Exception processing", t);
					
					try {
						Thread.sleep(20 * 1000);
					}
					catch (InterruptedException ie) {
						// So what ?
					}
				}
			}
		}
	}

	/**
	 * Create a watcher for a given file name
	 * 
	 * @param fileName path to the file to monitor
	 * @param howOften How often to verify for a change ( in millis )
	 * @throws Exception
	 */
	public FileWatchdog(final String fileName, final long howOften) throws Exception {
		this(new File(fileName), howOften);
	}

	/**
	 * Get the file that is monitored for changes
	 * 
	 * @return the file that is monitored for changes
	 */
	public File getFile() {
		return this.fileToWatch;
	}

	/**
	 * Create a watcher for a given file
	 * 
	 * @param f
	 * @param howOften -
	 *            How often to verify for a change ( in millis )
	 * @throws Exception
	 */
	public FileWatchdog(final File f, final long howOften) throws Exception {
		if (f == null) {
			throw new IllegalArgumentException("Cannot monitor a null File...");
		}
		
		this.cannonicalFilePath = f.getCanonicalPath();
		
		if (!f.exists()) {
			throw new Exception("The file [ " + this.cannonicalFilePath + " ] doesn't exist!");
		}
		
		if (!f.canRead()) {
			throw new Exception("The file [ " + this.cannonicalFilePath + " ] doesn't have Read acces!");
		}
		
		this.seq = SEQ.getAndIncrement();

		this.TIME_TO_SLEEP = howOften;

		if (this.TIME_TO_SLEEP <= 0) {
			this.TIME_TO_SLEEP = 20 * 1000;//every 20s
		}

		this.fileToWatch = f;
		this.lastModifiedTime = this.fileToWatch.lastModified();
		
		synchronized (lock) {
			if (dfwManager == null) {
				dfwManager = new DFWManager();
				dfwManager.start();
			}
		}
				
		dfwManager.add(this, System.currentTimeMillis() + this.TIME_TO_SLEEP);
	}

	/**
	 * Stop monitoring this file for changes 
	 */
	public void stopIt() {
		if (dfwManager!=null)
			dfwManager.remove(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		stopIt();
		super.finalize();
	}
	
	@Override
	public void setChanged(){
		super.setChanged();
	}
	
}

