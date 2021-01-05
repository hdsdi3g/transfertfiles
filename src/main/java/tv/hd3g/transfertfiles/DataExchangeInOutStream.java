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
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Not reusable
 */
public class DataExchangeInOutStream {
	private static final Logger log = LogManager.getLogger();

	private final InternalInputStream internalInputStream;
	private final InternalOutputStream internalOutputStream;

	private final ByteBuffer buffer;
	private volatile boolean stopped;
	private volatile boolean inWrite;

	public DataExchangeInOutStream(final int bufferSize) {
		internalInputStream = new InternalInputStream();
		internalOutputStream = new InternalOutputStream();
		stopped = false;
		inWrite = true;
		buffer = ByteBuffer.allocate(bufferSize);
		buffer.limit(0);
	}

	public DataExchangeInOutStream() {
		this(8192);
	}

	private class InternalInputStream extends InputStream {
		private volatile boolean closed = false;

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0) {
				throw new IllegalArgumentException("Invalid len=" + len);
			}

			while (inWrite
			       && internalOutputStream.closed == false
			       && closed == false
			       && stopped == false) {
				Thread.onSpinWait();
			}
			if (closed) {
				throw new IOException("Closed InputStream");
			} else if (stopped) {
				closed = true;
				return -1;
			}

			final var toRead = Math.min(buffer.remaining(), len);
			if (toRead == 0) {
				inWrite = true;
				if (internalOutputStream.closed) {
					log.trace("outstream was close");
					return -1;
				}
				log.trace("read 0 remaining={}", buffer.remaining());
				return 0;
			} else {
				log.trace("Read from remaining={} toRead={} to b={} off={} len={}",
				        buffer.remaining(), toRead, b.length, off, len);
				buffer.get(b, off, toRead);
				if (buffer.hasRemaining() == false) {
					inWrite = true;
				}
				return toRead;
			}
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
			if (closed || stopped) {
				return 0;
			}
			if (inWrite == false) {
				return buffer.remaining();
			}
			return 0;
		}

		@Override
		public void close() throws IOException {
			if (closed) {
				return;
			}
			closed = true;
			buffer.clear();
			internalOutputStream.close();
			log.trace("Stop read");
		}

	}

	private class InternalOutputStream extends OutputStream {
		private volatile boolean closed = false;

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0) {
				throw new IllegalArgumentException("Invalid len=" + len);
			}

			var toWrite = len;
			var writed = 0;

			while (closed == false && stopped == false && toWrite > 0) {
				while (inWrite == false) {
					Thread.onSpinWait();
				}
				buffer.clear();

				final var writedOnLoop = Math.min(toWrite, buffer.remaining());

				log.trace("Write from b={} off={} len={} to writedOnLoop={} toWrite={}",
				        b.length, off, len, writedOnLoop, toWrite);

				buffer.put(b, off + writed, writedOnLoop);
				buffer.flip();
				toWrite = toWrite - writedOnLoop;
				writed = writed + writedOnLoop;
				inWrite = false;
			}

			if (closed) {
				throw new IOException("Closed OutputStream");
			} else if (stopped) {
				throw new IOException("Stopped OutputStream");
			}
		}

		@Override
		public void write(final int b) throws IOException {
			final var oneByte = new byte[] { (byte) b };
			write(oneByte, 0, 1);
		}

		@Override
		public void close() throws IOException {
			if (closed) {
				return;
			}
			closed = true;
			log.trace("Stop write");
		}

	}

	public int getBufferSize() {
		return buffer.capacity();
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

	public synchronized boolean isStopped() {
		return stopped;
	}

	public synchronized void stop() {
		stopped = true;
	}
}
