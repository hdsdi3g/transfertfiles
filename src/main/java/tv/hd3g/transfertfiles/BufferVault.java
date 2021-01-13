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

import java.nio.ByteBuffer;
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
		} else if (missingSize == 0) {
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

}
