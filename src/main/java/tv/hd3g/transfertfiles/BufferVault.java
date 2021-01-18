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

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Thread safe
 */
public class BufferVault {
	private byte[] datas;
	private int size;
	private boolean justWrite;

	public BufferVault() {
		datas = new byte[0];
		size = 0;
		justWrite = false;
	}

	public BufferVault(final int baseInternalArraySize) {
		datas = new byte[baseInternalArraySize];
		size = 0;
		justWrite = false;
	}

	public synchronized BufferVault copy() {
		final var buffer = new BufferVault(size);
		buffer.size = size;
		System.arraycopy(datas, 0, buffer.datas, 0, size);
		return buffer;
	}

	/**
	 * Short end for ByteBuffer.wrap and BufferVault.wrap(ByteBuffer)
	 * @param byteArrayToHeaByteBuffer
	 * @return
	 */
	public static BufferVault wrap(final byte[] byteArrayToHeapByteBuffer) {
		return new BufferVault().write(byteArrayToHeapByteBuffer);
	}

	private static void checkBufferArray(final byte[] buffer, final int pos, final int len) {
		Objects.checkFromIndexSize(pos, len, buffer.length);
	}

	public synchronized BufferVault ensureBufferSize(final int itemsCountToAdd) {
		final var missingSize = size + itemsCountToAdd - datas.length;
		if (itemsCountToAdd < 0) {
			throw new IllegalArgumentException("Invalid itemsCountToAdd=" + itemsCountToAdd);
		} else if (missingSize <= 0) {
			return this;
		}
		final var newData = new byte[datas.length + missingSize];
		System.arraycopy(datas, 0, newData, 0, size);
		datas = newData;
		justWrite = true;
		return this;
	}

	public synchronized void clear() {
		size = 0;
		justWrite = true;
	}

	/**
	 * @return this
	 */
	public synchronized BufferVault write(final ByteBuffer buffer) {
		if (buffer.hasRemaining() == false) {
			return this;
		}
		justWrite = true;
		final var remaining = buffer.remaining();
		ensureBufferSize(remaining);
		buffer.get(datas, size, remaining);
		size += remaining;
		return this;
	}

	/**
	 * @return this
	 */
	public synchronized BufferVault write(final byte[] buffer, final int pos, final int len) {
		if (len == 0) {
			return this;
		}
		checkBufferArray(buffer, pos, len);
		justWrite = true;
		ensureBufferSize(len);
		System.arraycopy(buffer, pos, datas, size, len);
		size += len;
		return this;
	}

	/**
	 * @return this
	 */
	public synchronized BufferVault write(final byte[] buffer) {
		return write(buffer, 0, buffer.length);
	}

	public synchronized int getSize() {
		return size;
	}

	/**
	 * @return empty if not datas to read.
	 */
	public synchronized byte[] readAll() {
		final var array = new byte[size];
		System.arraycopy(datas, 0, array, 0, size);
		return array;
	}

	/**
	 * @param b the array to feed with datas
	 * @param pos where start to read data in internal array
	 * @param off where to put data in b
	 * @param len max data len to put in b
	 * @return real data len readed from internal array, -1 no datas/pos to big
	 */
	public synchronized int read(final byte[] b, final int pos, final int off, final int len) {
		Objects.checkFromIndexSize(off, len, b.length);
		if (pos >= size) {
			return -1;
		} else if (len <= 0) {
			return 0;
		}
		final var lenToRead = Math.min(len, size - pos);
		System.arraycopy(datas, pos, b, off, lenToRead);
		return lenToRead;
	}

	public synchronized int read(final int pos) {
		if (pos >= size) {
			return -1;
		} else if (pos < 0) {
			throw new IndexOutOfBoundsException("Can't access to negative positions: " + pos);
		}
		return datas[pos] & 0xFF;
	}

	/**
	 * @return empty if not datas to read.
	 */
	public synchronized ByteBuffer readAllToByteBuffer() {
		return ByteBuffer.allocate(size).put(datas, 0, size).flip();
	}

	/**
	 * Internal buffer will be reused for each next
	 */
	public synchronized Iterator<byte[]> iterator(final int bufferSize) {
		justWrite = false;
		return new Itr(bufferSize);
	}

	/**
	 * Internal buffer will be reused for each item
	 */
	public synchronized Stream<byte[]> stream(final int bufferSize) {
		return StreamSupport.stream(Spliterators.spliterator(
		        iterator(bufferSize), size, IMMUTABLE + ORDERED + SIZED + NONNULL),
		        false);
	}

	private class Itr implements Iterator<byte[]> {

		private final byte[] bArray;
		private volatile int readIndex;

		Itr(final int bufferSize) {
			bArray = new byte[bufferSize];
			readIndex = 0;
		}

		@Override
		public synchronized boolean hasNext() {
			if (justWrite) {
				throw new IllegalStateException("You can't iterate just after write. This instance is obsolete");
			}
			return readIndex + 1 <= size;
		}

		@Override
		public synchronized byte[] next() {
			if (justWrite) {
				throw new IllegalStateException("You can't iterate just after write. This instance is obsolete");
			}
			byte[] array;
			if (readIndex + 1 > size) {
				throw new NoSuchElementException();
			} else if (size - readIndex < bArray.length) {
				array = new byte[size - readIndex];
			} else {
				array = bArray;
			}
			System.arraycopy(datas, readIndex, array, 0, array.length);
			readIndex += array.length;
			return array;
		}
	}

	public OutputStream asOutputStream() {
		final var ref = this;
		return new OutputStream() {

			@Override
			public void write(final int b) throws IOException {
				ref.write(new byte[] { (byte) b });
			}

			@Override
			public void write(final byte[] b) throws IOException {
				ref.write(b);
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				ref.write(b, off, len);
			}
		};
	}

	/**
	 * Don't close/flush after write.
	 * Inject direcly the internal byte array. If outStream change it, it will change internally.
	 * @return this
	 */
	public synchronized BufferVault read(final OutputStream outStream) throws IOException {
		outStream.write(datas, 0, size);
		return this;
	}

	/**
	 * Don't close/flush after read, call available before start read to pre-heat internal buffer
	 * @param inStream
	 * @return total transferred bytes
	 */
	public synchronized int write(final InputStream inStream, final int bufferSize) throws IOException {
		ensureBufferSize(inStream.available());
		/**
		 * See InputStream.transferTo
		 */
		var transferred = 0;
		final var buffer = new byte[bufferSize];
		int read;
		while ((read = inStream.read(buffer, 0, buffer.length)) >= 0) {
			write(buffer, 0, read);
			transferred += read;
		}
		return transferred;
	}

	@Override
	public int hashCode() {
		var result = 1;
		for (var pos = 0; pos < size; pos++) {
			result = 31 * result + datas[pos];
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (BufferVault) obj;
		return Arrays.equals(datas, 0, size, other.datas, 0, other.size);
	}

	/**
	 * Remove all internal datas betwen 0 and pos, add append (copy/write) newDataSource content.
	 */
	public synchronized void compactAndAppend(final int pos, final BufferVault inserted) {
		if (pos < 0) {
			throw new IllegalArgumentException("Invalid negative pos: " + pos);
		}
		justWrite = true;

		if (pos >= size) {
			clear();
			write(inserted.datas);
		} else if (datas.length == 0 || pos == 0) {
			write(inserted.datas);
		} else if (pos + inserted.size > datas.length) {
			final var datasLarger = new byte[size - pos + inserted.size];
			System.arraycopy(datas, pos, datasLarger, 0, size - pos);
			datas = datasLarger;
			System.arraycopy(inserted.datas, 0, datas, size - pos, inserted.size);
			size = datas.length;
		} else {
			final var newDatas = new byte[size - pos + inserted.size];
			System.arraycopy(datas, pos, newDatas, 0, size - pos);
			System.arraycopy(inserted.datas, 0, newDatas, size - pos, inserted.size);

			datas = newDatas;
			size = newDatas.length;
		}
	}

}
