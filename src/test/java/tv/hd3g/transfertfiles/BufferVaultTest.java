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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferVaultTest {
	static Random random;

	@BeforeAll
	static void initAll() {
		random = new Random();
	}

	BufferVault vault;
	byte[] inputArray;

	@BeforeEach
	void init() throws Exception {
		inputArray = new byte[random.nextInt(100) + 100];
		random.nextBytes(inputArray);
	}

	@Nested
	class DefaultSizeInternalArray {

		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault();
		}

		@Test
		void testCopy() {
			final var vault0 = vault.write(inputArray).copy();
			assertTrue(Arrays.equals(inputArray, vault0.readAll()));
		}

		@Test
		void testWrapByteArray() {
			assertTrue(Arrays.equals(inputArray, BufferVault.wrap(inputArray).readAll()));
		}

		@Test
		void testClear() {
			vault.write(inputArray);
			vault.clear();
			assertEquals(0, vault.getSize());
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
		}

		@Test
		void testWriteByteBuffer() {
			vault.write(ByteBuffer.wrap(inputArray));
			assertTrue(Arrays.equals(inputArray, vault.readAll()));
		}

		@Test
		void testWriteByteBuffer_Empty() {
			vault.write(ByteBuffer.allocate(0));
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
		}

		@Test
		void testWriteByteArrayIntInt() {
			vault.write(inputArray, 10, 20);
			assertTrue(Arrays.equals(inputArray, 10, 30, vault.readAll(), 0, 20));
		}

		@Test
		void testWriteByteArrayIntInt_Empty() {
			vault.write(inputArray, 0, 0);
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
		}

		@Test
		void testWriteByteArray() {
			vault.write(inputArray);
			assertTrue(Arrays.equals(inputArray, vault.readAll()));
		}

		@Test
		void testWriteByteArray_Empty() {
			vault.write(new byte[0]);
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
		}

		@Test
		void testGetSize() {
			vault.write(inputArray);
			assertEquals(inputArray.length, vault.getSize());
			vault.write(inputArray);
			assertEquals(inputArray.length * 2, vault.getSize());
		}

		@Test
		void testReadAll() {
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
			vault.write(inputArray);
			assertTrue(Arrays.equals(inputArray, vault.readAll()));
			vault.clear();
			assertTrue(Arrays.equals(new byte[0], vault.readAll()));
		}

		@Test
		void testReadAllToByteBuffer() {
			var bb = vault.readAllToByteBuffer();
			var bbA = new byte[bb.remaining()];
			bb.get(bbA);
			assertTrue(Arrays.equals(new byte[0], bbA));

			vault.write(inputArray);
			bb = vault.readAllToByteBuffer();
			bbA = new byte[bb.remaining()];
			bb.get(bbA);
			assertTrue(Arrays.equals(inputArray, bbA));
		}

		@Test
		void testIterator() {
			vault.write(inputArray);
			final var itr1 = vault.iterator(inputArray.length);
			assertTrue(itr1.hasNext());
			assertTrue(Arrays.equals(inputArray, itr1.next()));

			assertFalse(itr1.hasNext());
			assertThrows(NoSuchElementException.class, () -> itr1.next());
		}

		@Test
		void testIterator_empty() {
			final var itr0 = vault.iterator(10);
			assertFalse(itr0.hasNext());
			assertThrows(NoSuchElementException.class, () -> itr0.next());
		}

		@Test
		void testIterator_smallerBuffer() {
			vault.write(inputArray);
			final var itr1 = vault.iterator(inputArray.length / 11);

			final var outputBB = ByteBuffer.allocate(inputArray.length);
			itr1.forEachRemaining(outputBB::put);
			outputBB.flip();
			assertEquals(inputArray.length, outputBB.remaining());

			final var outputArray = new byte[inputArray.length];
			outputBB.get(outputArray);
			assertTrue(Arrays.equals(inputArray, outputArray));

			assertFalse(itr1.hasNext());
			assertThrows(NoSuchElementException.class, () -> itr1.next());
		}

		@Test
		void testStream() {
			final var emptySize = vault.stream(inputArray.length / 7)
			        .mapToInt(array -> array.length)
			        .sum();
			assertEquals(0, emptySize);

			vault.write(inputArray);

			final var count = new AtomicInteger();
			final var fullSize = vault.stream(inputArray.length / 7)
			        .peek(array -> count.incrementAndGet())
			        .mapToInt(array -> array.length)
			        .sum();
			assertEquals(inputArray.length, fullSize);
			assertTrue(count.get() > 0);
			assertTrue(count.get() < inputArray.length);

			final var outputBB = ByteBuffer.allocate(inputArray.length);
			vault.stream(inputArray.length / 7).forEach(outputBB::put);
			outputBB.flip();
			assertEquals(inputArray.length, outputBB.remaining());

			final var outputArray = new byte[inputArray.length];
			outputBB.get(outputArray);
			assertTrue(Arrays.equals(inputArray, outputArray));
		}

		@Test
		void testIterator_WriteDuringRead() {
			vault.write(inputArray);
			final var itr0 = vault.iterator(10);
			vault.write(new byte[0]);
			assertTrue(itr0.hasNext());
			assertTrue(Arrays.equals(inputArray, 0, 10, itr0.next(), 0, 10));

			vault.write(inputArray);
			assertThrows(IllegalStateException.class, () -> itr0.hasNext());
			assertThrows(IllegalStateException.class, () -> itr0.next());
		}

		@Test
		void testReadByteArray_fully() {
			vault.write(inputArray);
			final var outputArray = new byte[inputArray.length];
			final var readed = vault.read(outputArray, 0, 0, inputArray.length);
			assertEquals(readed, inputArray.length);
			assertTrue(Arrays.equals(inputArray, outputArray));
		}

		@Test
		void testReadByteArray_partial() {
			vault.write(inputArray);
			final var outputArray = new byte[inputArray.length / 2];
			final var posIn = inputArray.length / 10;
			final var posOut = outputArray.length / 10;
			final var sizeOut = outputArray.length - posOut;
			final var readed = vault.read(outputArray, posIn, posOut, sizeOut);
			assertEquals(sizeOut, readed);
			assertTrue(Arrays.equals(inputArray, posIn, posIn + sizeOut, outputArray, posOut, posOut + sizeOut));
		}

		@Test
		void testReadByteArray_bounds() {
			vault.write(inputArray);
			final var outputArray = new byte[inputArray.length];
			var readed = vault.read(outputArray, inputArray.length * 2, 0, inputArray.length);
			assertEquals(-1, readed);

			readed = vault.read(outputArray, 0, 0, 0);
			assertEquals(0, readed);
		}

		@Test
		void testAsOutputStream() throws IOException {
			final var baIS = new ByteArrayInputStream(inputArray);
			final var copied = baIS.transferTo(vault.asOutputStream());
			assertEquals(inputArray.length, copied);
			assertTrue(Arrays.equals(inputArray, vault.readAll()));

			var rByte = random.nextInt() & 0xFF;
			vault.asOutputStream().write(rByte);
			assertEquals(rByte, vault.read(inputArray.length));

			rByte = random.nextInt() & 0xFF;
			vault.asOutputStream().write(new byte[] { (byte) rByte });
			assertEquals(rByte, vault.read(inputArray.length + 1));

			rByte = random.nextInt() & 0xFF;
			vault.asOutputStream().write(new byte[] { (byte) (random.nextInt() & 0xFF), (byte) rByte }, 1, 1);
			assertEquals(rByte, vault.read(inputArray.length + 2));
		}

		@Test
		void testReadInt() throws IOException {
			vault.write(inputArray);
			assertEquals(-1, vault.read(inputArray.length * 2));
			assertThrows(IndexOutOfBoundsException.class, () -> vault.read(-1));
		}

		@Test
		void testOutputStream() throws IOException {
			vault.write(inputArray);
			final var outStream = new ByteArrayOutputStream();
			vault.read(outStream);
			assertTrue(Arrays.equals(inputArray, outStream.toByteArray()));
		}

		@Test
		void testWrite() throws IOException {
			final var inStream = new ByteArrayInputStream(inputArray);
			final var transfered = vault.write(inStream, inputArray.length / 10);
			assertTrue(Arrays.equals(inputArray, vault.readAll()));
			assertEquals(inputArray.length, transfered);
		}

		@Test
		void testHashCodeEquals() throws IOException {
			vault.write(inputArray);
			final var v0 = new BufferVault().write(inputArray);
			assertEquals(vault, v0);
			assertEquals(vault.hashCode(), v0.hashCode());

			vault.clear();
			final var v1 = new BufferVault();
			assertEquals(vault, v1);
			assertEquals(vault.hashCode(), v1.hashCode());

			vault = new BufferVault();
			assertEquals(vault, v1);
			assertEquals(vault.hashCode(), v1.hashCode());

			assertFalse(vault.equals(null)); // NOSONAR S5785
			assertEquals(vault, vault);

			class BVault extends BufferVault {
			}
			assertNotEquals(vault, new BVault());
		}

		@Nested
		class CompactAndAppendWithSmaller {

			byte[] additionnal;
			BufferVault newDataSource;

			@BeforeEach
			void init() throws Exception {
				additionnal = new byte[random.nextInt(inputArray.length - 2) + 1];
				random.nextBytes(additionnal);
				newDataSource = new BufferVault().write(additionnal);
			}

			@AfterEach
			void ends() throws Exception {
				assertEquals(new BufferVault().write(additionnal), newDataSource);
			}

			@Test
			void testAddTheEnd_FromEmpty() {
				vault.compactAndAppend(0, newDataSource);
				assertEquals(vault.getSize(), newDataSource.getSize());
				assertEquals(vault, newDataSource);
			}

			@Test
			void testAddTheEnd_FromFull() {
				vault.write(inputArray);
				vault.compactAndAppend(inputArray.length, newDataSource);

				final var compare = new BufferVault();
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddTheEnd_FromFull_withBiggerMargin() {
				vault.write(inputArray);
				vault.ensureBufferSize(additionnal.length);
				vault.compactAndAppend(inputArray.length, newDataSource);

				final var compare = new BufferVault();
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddTheEnd_FromFull_withSmallerMargin() {
				vault.write(inputArray);
				vault.ensureBufferSize(additionnal.length / 2);
				vault.compactAndAppend(inputArray.length, newDataSource);

				final var compare = new BufferVault();
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddTheStarts_FromFull() {
				vault.write(inputArray);
				vault.compactAndAppend(0, newDataSource);

				final var compare = new BufferVault();
				compare.write(inputArray);
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddTheStarts_FromFull_withBiggerMargin() {
				vault.write(inputArray);
				vault.ensureBufferSize(additionnal.length);
				vault.compactAndAppend(0, newDataSource);

				final var compare = new BufferVault();
				compare.write(inputArray);
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddTheStarts_FromFull_withSmallerMargin() {
				vault.write(inputArray);
				vault.ensureBufferSize(additionnal.length / 2);
				vault.compactAndAppend(0, newDataSource);

				final var compare = new BufferVault();
				compare.write(inputArray);
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddInMiddle_FromFull_withBiggerMargin() {
				vault.ensureBufferSize(inputArray.length + additionnal.length);
				vault.write(inputArray);
				final var middle = inputArray.length / 2;
				vault.compactAndAppend(middle, newDataSource);

				final var compare = new BufferVault();
				compare.write(inputArray, middle, inputArray.length - middle);
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testAddInMiddle_FromFull() {
				vault.write(inputArray);
				final var middle = inputArray.length / 2;
				vault.compactAndAppend(middle, newDataSource);

				final var compare = new BufferVault();
				compare.write(inputArray, middle, inputArray.length - middle);
				compare.write(additionnal);

				assertEquals(compare.getSize(), vault.getSize());
				assertEquals(compare, vault);
			}

			@Test
			void testNoNegativePos() {
				assertThrows(IllegalArgumentException.class, () -> vault.compactAndAppend(-1, newDataSource));
			}

		}

		@Nested
		class CompactAndAppendWithBigger extends CompactAndAppendWithSmaller {
			@Override
			@BeforeEach
			void init() throws Exception {
				additionnal = new byte[random.nextInt(100) + inputArray.length];
				random.nextBytes(additionnal);
				newDataSource = new BufferVault().write(additionnal);
			}

		}

	}

	@Nested
	class SmallerSizeInternalArray extends DefaultSizeInternalArray {

		@Override
		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault(inputArray.length / 2);
		}

	}

	@Nested
	class BiggerSizeInternalArray extends DefaultSizeInternalArray {

		@Override
		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault(inputArray.length * 2);
		}

	}

	@Nested
	class EnsureSmallSizeInternalArray extends DefaultSizeInternalArray {

		@Override
		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault();
			vault.ensureBufferSize(inputArray.length / 4);
		}

	}

	@Nested
	class EnsureBigSizeInternalArray extends DefaultSizeInternalArray {

		@Override
		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault();
			vault.ensureBufferSize(inputArray.length * 2);
		}

	}

	@Nested
	class Ensure0SizeInternalArray extends DefaultSizeInternalArray {

		@Override
		@BeforeEach
		void init() throws Exception {
			vault = new BufferVault();
			vault.ensureBufferSize(0);
		}

	}

	@Test
	void checkNegativeEnsureBufferSize() {
		vault = new BufferVault();
		assertThrows(IllegalArgumentException.class, () -> vault.ensureBufferSize(-1));
	}

}
