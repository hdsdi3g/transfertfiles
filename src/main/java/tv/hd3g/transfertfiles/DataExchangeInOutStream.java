/*
 * This file is part of transfertfiles.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.transfertfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.filters.DataExchangeFilter;

/**
 * Not reusable
 */
public class DataExchangeInOutStream {
	private static final Logger log = LogManager.getLogger();

	private final InternalInputStream internalInputStream;
	private final InternalOutputStream internalOutputStream;

	private final List<DataExchangeFilter> filters;
	private final ConcurrentLinkedQueue<ByteBuffer> readQueue;
	private final AtomicInteger ensureMinWriteBuffersSize;
	private final HashMap<DataExchangeFilter, Long> filterPerformance;
	private final HashMap<DataExchangeFilter, Long> filterDeltaThroughput;
	private final AtomicLong ioWaitTime;

	private volatile State state;

	public enum State {
		WORKING(false, false),
		STOPPED_BY_USER(true, false),
		STOPPED_BY_FILTER(true, false),
		WRITER_MANUALLY_CLOSED(false, true),
		FILTER_ERROR(false, true);

		final boolean stopped;
		final boolean close;

		State(final boolean stopped, final boolean close) {
			this.stopped = stopped;
			this.close = close;
		}
	}

	public DataExchangeInOutStream() {
		internalInputStream = new InternalInputStream();
		internalOutputStream = new InternalOutputStream();
		filters = Collections.synchronizedList(new ArrayList<>());
		readQueue = new ConcurrentLinkedQueue<>();
		state = State.WORKING;
		ensureMinWriteBuffersSize = new AtomicInteger();
		filterPerformance = new HashMap<>();
		filterDeltaThroughput = new HashMap<>();
		ioWaitTime = new AtomicLong(0);
	}

	private class InternalInputStream extends InputStream {
		private volatile boolean readerClosed = false;

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0) {
				throw new IllegalArgumentException("Invalid len=" + len);
			}

			if (log.isTraceEnabled()) {
				log.trace("Read event (wait) of {} byte(s), {} in queue...", len, readQueue.size());
			}

			while (readQueue.isEmpty()
			       && state == State.WORKING
			       && readerClosed == false) {
				Thread.onSpinWait();
			}

			if (readerClosed) {
				throw new IOException("Closed InputStream (reader)");
			} else if (state.stopped) {
				log.trace("Read stopped: {}, {} in queue", state, readQueue.size());
				readerClosed = true;
				return -1;
			} else if (readQueue.isEmpty() && state.close) {
				log.trace("Read: outstream (reader) was close, nothing in queue");
				return -1;
			}

			final var buffer = readQueue.element();

			final var toRead = Math.min(buffer.remaining(), len);
			log.trace("Read from remaining={} toRead={} to b={} off={} len={}",
			        buffer.remaining(), toRead, b.length, off, len);

			final var now = System.currentTimeMillis();
			buffer.get(b, off, toRead);
			ioWaitTime.addAndGet(System.currentTimeMillis() - now);

			if (buffer.hasRemaining() == false) {
				readQueue.remove();
			}

			return toRead;
		}

		@Override
		public int read() throws IOException {
			final var oneByte = new byte[1];
			final var size = read(oneByte, 0, 1);
			if (size == 1) {
				return oneByte[0] & 0xFF;
			}
			return -1;
		}

		@Override
		public int available() throws IOException {
			if (readerClosed || state.stopped) {
				return 0;
			}
			return (int) readQueue.stream()
			        .mapToInt(ByteBuffer::remaining)
			        .summaryStatistics()
			        .getSum();
		}

		@Override
		public void close() throws IOException {
			if (readerClosed) {
				return;
			}
			readerClosed = true;

			internalOutputStream.close();

			readQueue.forEach(ByteBuffer::clear);
			log.trace("Close read");
		}

	}

	private class InternalOutputStream extends OutputStream {

		private final BufferVault buffers;

		InternalOutputStream() {
			buffers = new BufferVault();
		}

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0) {
				throw new IllegalArgumentException("Invalid len=" + len);
			}

			if (state == State.WORKING) {
				while (readQueue.isEmpty() == false) {
					Thread.onSpinWait();
				}

				final var now = System.currentTimeMillis();
				buffers.write(b, off, len);
				ioWaitTime.addAndGet(System.currentTimeMillis() - now);

				final var totalWrited = buffers.getSize();
				log.trace("Write from b/off/len {}/{}/{} to total writed {}",
				        b.length, off, len, totalWrited);

				if (totalWrited > ensureMinWriteBuffersSize.get()) {
					processFilters(false);
				}
			}

			if (state == State.STOPPED_BY_FILTER) {
				throw new IOException("Stopped OutputStream (writer) by filter");
			} else if (state == State.STOPPED_BY_USER) {
				throw new IOException("Stopped OutputStream (writer)");
			} else if (state == State.WRITER_MANUALLY_CLOSED) {
				throw new IOException("Closed OutputStream (writer)");
			} else if (state == State.FILTER_ERROR) {
				throw new IOException("Closed OutputStream (writer) caused by filter error");
			}
		}

		@Override
		public void write(final int b) throws IOException {
			final var oneByte = new byte[] { (byte) b };
			write(oneByte, 0, 1);
		}

		@Override
		public void close() throws IOException {
			if (state.close) {
				return;
			}
			log.trace("Close write");

			processFilters(true);
			if (state == State.WORKING) {
				state = State.WRITER_MANUALLY_CLOSED;
			} else if (state == State.STOPPED_BY_FILTER) {
				throw new IOException("Stopped OutputStream (writer) by filter");
			} else if (state == State.STOPPED_BY_USER) {
				throw new IOException("Stopped OutputStream (writer)");
			} else if (state == State.FILTER_ERROR) {
				throw new IOException("Closed OutputStream (writer) caused by filter error");
			}
		}

		private void processFilters(final boolean lastCall) {
			var canceled = false;
			var nextBuffers = buffers;

			for (var posF = 0; posF < filters.size(); posF++) {
				final var currentFilter = filters.get(posF);
				if (canceled) {
					try {
						currentFilter.onCancelTransfert();
					} catch (final Exception e) {
						log.warn("Error during during close all filters", e);
					}
					continue;
				}

				try {
					if (log.isTraceEnabled()) {
						log.trace("Apply filter {} for {} bytes...",
						        currentFilter.getFilterName(), nextBuffers.getSize());
					}
					final var previousBuffers = nextBuffers;
					nextBuffers = applyFilter(lastCall, nextBuffers, currentFilter, previousBuffers);
				} catch (final StoppedByFilter e) {
					canceled = true;
					log.info("Filter manually stop exchange process {}", currentFilter.getFilterName());
					state = State.STOPPED_BY_FILTER;
				} catch (final Exception e) {
					canceled = true;
					log.error("Error during process filtering (close exchange process)", e);
					state = State.FILTER_ERROR;
				}
			}
			if (canceled == false) {
				readQueue.add(nextBuffers.readAllToByteBuffer());
				if (log.isTraceEnabled()) {
					log.trace("Filters: read queue has now {} item(s)", readQueue.size());
				}
				buffers.clear();
			}
		}

		private BufferVault applyFilter(final boolean lastCall,
		                                BufferVault nextBuffers,
		                                final DataExchangeFilter currentFilter,
		                                final BufferVault previousBuffers) throws IOException {
			long currentPerformance;
			long currentDeltaThroughput;
			long now;
			int inputBufferSize;

			currentPerformance = filterPerformance.computeIfAbsent(currentFilter, cF -> 0L);
			currentDeltaThroughput = filterDeltaThroughput.computeIfAbsent(currentFilter, cF -> 0L);
			inputBufferSize = nextBuffers.getSize();
			now = System.currentTimeMillis();

			nextBuffers = currentFilter.applyDataFilter(lastCall, nextBuffers);

			currentPerformance += System.currentTimeMillis() - now;
			if (nextBuffers == null) {
				if (log.isTraceEnabled()) {
					log.trace("After apply filter {}, want to stop!", currentFilter.getFilterName());
				}
				throw new StoppedByFilter(currentFilter);
			}

			filterPerformance.put(currentFilter, currentPerformance);
			currentDeltaThroughput += inputBufferSize - nextBuffers.getSize();
			filterDeltaThroughput.put(currentFilter, currentDeltaThroughput);

			if (nextBuffers.getSize() == 0) {
				if (log.isTraceEnabled()) {
					log.trace("After apply filter {}, no datas provided", currentFilter.getFilterName());
				}
				nextBuffers = previousBuffers;
			} else if (log.isTraceEnabled()) {
				log.trace("After apply filter {}, provide {} bytes",
				        currentFilter.getFilterName(), nextBuffers.getSize());
			}
			return nextBuffers;
		}
	}

	private class StoppedByFilter extends RuntimeException {
		StoppedByFilter(final DataExchangeFilter filter) {
			super(filter.getFilterName());
		}

	}

	public class TransfertStats {
		private final long totalDuration;
		private final long deltaTranfered;

		private TransfertStats(final long totalDuration, final long deltaTranfered) {
			this.totalDuration = totalDuration;
			this.deltaTranfered = deltaTranfered;
		}

		/**
		 * @return in bytes, relative to in filter input
		 *         (0 = same as input, negative for shrink, positive for expand);
		 */
		public long getDeltaTranfered() {
			return deltaTranfered;
		}

		/**
		 * @return in ms
		 */
		public long getTotalDuration() {
			return totalDuration;
		}
	}

	public synchronized TransfertStats getTransfertStats(final DataExchangeFilter filter) {
		if (state == State.WORKING) {
			throw new IllegalStateException("Can't access to transfert stats during processing...");
		}
		return new TransfertStats(filterPerformance.computeIfAbsent(filter, f -> 0L),
		        filterDeltaThroughput.computeIfAbsent(filter, f -> 0L));
	}

	/**
	 * @return must be used by a separate Thread from getDestOriginStream()
	 *         Never forget to close it after push all datas to it.
	 */
	public OutputStream getDestTargetStream() {
		return internalOutputStream;
	}

	/**
	 * @return must be used by a separate Thread from getSourceTargetStream()
	 */
	public InputStream getSourceOriginStream() {
		return internalInputStream;
	}

	public synchronized void stop() {
		if (state == State.WORKING) {
			state = State.STOPPED_BY_USER;
		}
	}

	/**
	 * @return in ms
	 */
	public long getIoWaitTime() {
		return ioWaitTime.get();
	}

	public synchronized State getState() {
		return state;
	}

	public DataExchangeInOutStream addFilter(final DataExchangeFilter filter) {
		Objects.requireNonNull(filter);
		filters.add(filter);
		final var buffersSize = ensureMinWriteBuffersSize.updateAndGet(current -> {
			final var filterBuffer = filter.ensureMinDataSourcesDataLength();
			if (filterBuffer > current) {
				return filterBuffer;
			} else {
				return current;
			}
		});

		/**
		 * Pre-heat internalOutputStream.buffers internal size
		 */
		final var itemsCountToAdd = buffersSize - internalOutputStream.buffers.getSize();
		if (itemsCountToAdd > 0) {
			internalOutputStream.buffers.ensureBufferSize(itemsCountToAdd);
		}
		return this;
	}
}
