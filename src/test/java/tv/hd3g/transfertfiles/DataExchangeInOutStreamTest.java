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

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tv.hd3g.commons.IORuntimeException;

class DataExchangeInOutStreamTest {

	static Random random;
	DataExchangeInOutStream exchange;

	@BeforeAll
	static void initAll() {
		random = new Random();
	}

	@Test
	void testBaseCopy() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream(20);
		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var readerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		readerCF.orTimeout(2, TimeUnit.SECONDS);

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
	}

	@Test
	void testSimpleCopy() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream(10);
		final var dataInput = "0123456789ABCDEFGHIJklmnopqrstUVWXYZ".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var readerCF = CompletableFuture.runAsync(() -> {
			try {
				final var half = dataInput.length / 2;
				exchange.getDestTargetStream().write(dataInput, 0, half);
				exchange.getDestTargetStream().write(dataInput, half, dataInput.length - half);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		readerCF.orTimeout(2, TimeUnit.SECONDS);

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
	}

	@Test
	void testCopyBigger() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream(20);
		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length];

		final var readerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		readerCF.orTimeout(2, TimeUnit.SECONDS);

		assertEquals(dataInput.length, copiedSize);
		assertTrue(Arrays.areEqual(dataInput, dataOutput));
	}

	@Test
	void testInt() throws IOException {
		exchange = new DataExchangeInOutStream(1);

		final var byteOne = new byte[1];
		for (var pos = 0; pos < 128; pos++) {
			random.nextBytes(byteOne);
			final var number = byteOne[0] & 0xFF;
			exchange.getDestTargetStream().write(number);
			final var result = exchange.getSourceOriginStream().read();
			assertEquals(result, number);
		}

	}

	@Test
	void testBuffers() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final var sourceBuffers = IntStream.range(0, random.nextInt(100))
		        .map(i -> random.nextInt(1000) + 1)
		        .mapToObj(iR -> new byte[iR])
		        .collect(toUnmodifiableList());
		sourceBuffers.forEach(random::nextBytes);

		final var destBuffers = sourceBuffers.stream()
		        .map(buf -> buf.length)
		        .map(iR -> new byte[iR])
		        .collect(toList());
		Collections.shuffle(destBuffers);
		assertEquals(sourceBuffers.size(), destBuffers.size());

		exchange = new DataExchangeInOutStream(100 + random.nextInt(300));

		final var readerCF = CompletableFuture.runAsync(() -> {
			try {
				for (var pos = 0; pos < sourceBuffers.size(); pos++) {
					exchange.getDestTargetStream().write(sourceBuffers.get(pos));
				}
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			}
		});

		for (var pos = 0; pos < destBuffers.size(); pos++) {
			read(exchange.getSourceOriginStream(), destBuffers.get(pos));
		}
		readerCF.orTimeout(2, TimeUnit.SECONDS);

		final var sourceValues = sourceBuffers.stream()
		        .flatMap(byteArrayToListValues)
		        .collect(Collectors.toUnmodifiableList());
		final var destValues = destBuffers.stream()
		        .flatMap(byteArrayToListValues)
		        .collect(Collectors.toUnmodifiableList());

		assertEquals(sourceValues.size(), destValues.size());
		for (var pos = 0; pos < sourceValues.size(); pos++) {
			assertEquals(sourceValues.get(pos), destValues.get(pos), "Err pos " + pos);
		}
	}

	private static Function<byte[], Stream<Byte>> byteArrayToListValues = buf -> {
		final var list = new ArrayList<Byte>(buf.length);
		for (var pos = 0; pos < buf.length; pos++) {
			list.add(buf[pos]);
		}
		return list.stream();
	};

	/**
	 * From IOUtils
	 */
	public static int read(final InputStream input,
	                       final byte[] buffer) throws IOException {
		var remaining = buffer.length;
		while (remaining > 0) {
			final var location = buffer.length - remaining;
			final var count = input.read(buffer, location, remaining);
			if (count == -1) {
				break;
			}
			remaining -= count;
		}
		return buffer.length - remaining;
	}

	@Nested
	class Next {
		byte[] dataInput;
		OutputStream sourceTargetStream;
		InputStream destOriginStream;

		@BeforeEach
		void init() throws Exception {
			exchange = new DataExchangeInOutStream(5);
			dataInput = new byte[exchange.getBufferSize()];
			sourceTargetStream = exchange.getDestTargetStream();
			destOriginStream = exchange.getSourceOriginStream();
		}

		@Test
		void testInvalidSizes() throws IOException {
			assertThrows(IndexOutOfBoundsException.class, () -> destOriginStream.read(dataInput, 10_000, 1));
			assertThrows(IndexOutOfBoundsException.class, () -> destOriginStream.read(dataInput, 0, 10_000));
			assertThrows(IndexOutOfBoundsException.class, () -> sourceTargetStream.write(dataInput, 10_000, 1));
			assertThrows(IndexOutOfBoundsException.class, () -> sourceTargetStream.write(dataInput, 0, 10_000));
			assertThrows(IllegalArgumentException.class, () -> destOriginStream.read(dataInput, 0, 0));
			assertThrows(IllegalArgumentException.class, () -> sourceTargetStream.write(dataInput, 0, 0));
		}

		@Test
		void testReadAfterRead() throws IOException {
			sourceTargetStream.write(dataInput);
			for (var pos = 0; pos < dataInput.length; pos++) {
				assertTrue(destOriginStream.read() > -1);
			}
		}
	}

	@Nested
	class CloseBeforeEnds {
		byte[] dataInput;
		byte[] dataOutput;
		OutputStream sourceTargetStream;
		InputStream destOriginStream;

		@BeforeEach
		void init() throws Exception {
			exchange = new DataExchangeInOutStream(5);
			dataInput = new byte[exchange.getBufferSize()];
			dataOutput = new byte[dataInput.length];
			sourceTargetStream = exchange.getDestTargetStream();
			destOriginStream = exchange.getSourceOriginStream();
		}

		@Test
		void testCloseWriteDuringRead() throws IOException {
			sourceTargetStream.write(dataInput);
			read(exchange.getSourceOriginStream(), dataOutput);
			sourceTargetStream.close();
			assertEquals(-1, destOriginStream.read());
		}

		@Test
		void testCloseReadDuringWrite() throws IOException {
			sourceTargetStream.write(dataInput);
			read(destOriginStream, dataOutput);
			destOriginStream.close();
			assertThrows(IOException.class, () -> destOriginStream.read());
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
		}

		@Test
		void testCloseWriteBeforeRead() throws IOException {
			sourceTargetStream.write(dataInput);
			sourceTargetStream.close();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(exchange.getBufferSize(), read(destOriginStream, dataOutput));
			assertEquals(-1, destOriginStream.read());
		}

		@Test
		void testCloseWriteDuringRead2() throws IOException {
			final var dataInput = new byte[exchange.getBufferSize() * 4];

			final var cfWrite = CompletableFuture.runAsync(() -> {
				try {
					sourceTargetStream.write(dataInput);
				} catch (final IOException e) {
					throw new IORuntimeException(e);
				}
			});
			sourceTargetStream.close();
			read(destOriginStream, dataOutput);

			Exception e = null;
			try {
				cfWrite.get(1, SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				e = e1;
			}
			assertNotNull(e);
			assertTrue(e instanceof ExecutionException);
			assertTrue(e.getCause() instanceof IORuntimeException);
			assertTrue(e.getCause().getCause() instanceof IOException);
		}

		@Test
		void testCloseReadBeforeWrite() throws IOException {
			destOriginStream.close();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
		}

		@Test
		void testCloseTwice() throws IOException {
			destOriginStream.close();
			assertDoesNotThrow(() -> destOriginStream.close());
			sourceTargetStream.close();
			assertDoesNotThrow(() -> sourceTargetStream.close());
		}
	}

	@Nested
	class StopBeforeEnds {
		byte[] dataInput;
		byte[] dataOutput;
		OutputStream sourceTargetStream;
		InputStream destOriginStream;

		@BeforeEach
		void init() throws Exception {
			exchange = new DataExchangeInOutStream(5);
			dataInput = new byte[exchange.getBufferSize()];
			dataOutput = new byte[dataInput.length];
			sourceTargetStream = exchange.getDestTargetStream();
			destOriginStream = exchange.getSourceOriginStream();
		}

		@Test
		void testStopWriteDuringRead() throws IOException {
			sourceTargetStream.write(dataInput);
			read(exchange.getSourceOriginStream(), dataOutput);
			exchange.stop();
			assertEquals(-1, destOriginStream.read());
		}

		@Test
		void testStopReadDuringWrite() throws IOException {
			sourceTargetStream.write(dataInput);
			read(destOriginStream, dataOutput);
			exchange.stop();
			assertEquals(-1, destOriginStream.read());
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
		}

		@Test
		void testStopWriteBeforeRead() throws IOException {
			sourceTargetStream.write(dataInput);
			exchange.stop();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(0, read(destOriginStream, dataOutput));
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertThrows(IOException.class, () -> destOriginStream.read());
		}

		@Test
		void testStopWriteDuringRead2() throws IOException {
			final var dataInput = new byte[exchange.getBufferSize() * 4];

			final var cfWrite = CompletableFuture.runAsync(() -> {
				try {
					sourceTargetStream.write(dataInput);
				} catch (final IOException e) {
					throw new IORuntimeException(e);
				}
			});
			exchange.stop();
			read(destOriginStream, dataOutput);

			Exception e = null;
			try {
				cfWrite.get(1, SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				e = e1;
			}
			assertNotNull(e);
			assertTrue(e instanceof ExecutionException);
			assertTrue(e.getCause() instanceof IORuntimeException);
			assertTrue(e.getCause().getCause() instanceof IOException);
		}

		@Test
		void testStopReadBeforeWrite() throws IOException {
			exchange.stop();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
		}

		@Test
		void testStopTwice() throws IOException {
			exchange.stop();
			assertDoesNotThrow(() -> exchange.stop());
		}

		@Test
		void testStopWithClose() throws IOException {
			exchange.stop();
			assertDoesNotThrow(() -> destOriginStream.close());
			assertDoesNotThrow(() -> sourceTargetStream.close());
		}
	}

	@Test
	void testGetBufferSize() {
		exchange = new DataExchangeInOutStream();
		assertEquals(8192, exchange.getBufferSize());
		final var buffSize = Math.abs(random.nextInt());
		exchange = new DataExchangeInOutStream(buffSize);
		assertEquals(buffSize, exchange.getBufferSize());
	}

	@Test
	void testIsStopped() throws IOException {
		exchange = new DataExchangeInOutStream();
		assertFalse(exchange.isStopped());
		exchange.getSourceOriginStream().close();
		assertFalse(exchange.isStopped());
		exchange.getDestTargetStream().close();
		assertFalse(exchange.isStopped());

		exchange.stop();
		assertTrue(exchange.isStopped());
		exchange.stop();
	}

	@Test
	void testReadAvaliable() throws IOException {
		exchange = new DataExchangeInOutStream(5);
		final var dataInput = new byte[exchange.getBufferSize()];
		final var dataOutput = new byte[dataInput.length];
		final var destOriginStream = exchange.getSourceOriginStream();

		assertEquals(0, destOriginStream.available());
		exchange.getDestTargetStream().write(dataInput);
		assertEquals(5, destOriginStream.available());
		destOriginStream.read();
		assertEquals(4, destOriginStream.available());
		destOriginStream.read(dataOutput);
		assertEquals(0, destOriginStream.available());
		assertEquals(0, destOriginStream.available());

		destOriginStream.close();
		assertEquals(0, destOriginStream.available());

		exchange = new DataExchangeInOutStream(5);
		destOriginStream.close();
		assertEquals(0, destOriginStream.available());

		exchange = new DataExchangeInOutStream(5);
		exchange.stop();
		assertEquals(0, destOriginStream.available());
	}

}
