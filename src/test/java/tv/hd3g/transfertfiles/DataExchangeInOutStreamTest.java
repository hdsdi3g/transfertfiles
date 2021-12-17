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
import static tv.hd3g.transfertfiles.DataExchangeInOutStream.State.FILTER_ERROR;
import static tv.hd3g.transfertfiles.DataExchangeInOutStream.State.STOPPED_BY_FILTER;
import static tv.hd3g.transfertfiles.DataExchangeInOutStream.State.STOPPED_BY_USER;
import static tv.hd3g.transfertfiles.DataExchangeInOutStream.State.WORKING;
import static tv.hd3g.transfertfiles.DataExchangeInOutStream.State.WRITER_MANUALLY_CLOSED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.DataExchangeInOutStream.State;
import tv.hd3g.transfertfiles.filters.DataExchangeFilter;

class DataExchangeInOutStreamTest {

	static Random random;
	DataExchangeInOutStream exchange;

	@BeforeAll
	static void initAll() {
		random = new Random();
	}

	@Test
	void testBaseCopy() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testBaseCopy_filtered() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new XorTestFilter()).addFilter(new XorTestFilter());

		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testSimpleCopy() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		final var dataInput = "0123456789ABCDEFGHIJklmnopqrstUVWXYZ".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				final var half = dataInput.length / 2;
				exchange.getDestTargetStream().write(dataInput, 0, half);
				exchange.getDestTargetStream().write(dataInput, half, dataInput.length - half);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testSimpleCopy_filtered() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new XorTestFilter()).addFilter(new XorTestFilter());

		final var dataInput = "0123456789ABCDEFGHIJklmnopqrstUVWXYZ".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				final var half = dataInput.length / 2;
				exchange.getDestTargetStream().write(dataInput, 0, half);
				exchange.getDestTargetStream().write(dataInput, half, dataInput.length - half);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize, new String(dataOutput));
		assertTrue(Arrays.areEqual(dataInput, dataOutput), new String(dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testCopyBigger() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize);
		assertTrue(Arrays.areEqual(dataInput, dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testCopyBigger_filtered() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new XorTestFilter()).addFilter(new XorTestFilter());

		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize);
		assertTrue(Arrays.areEqual(dataInput, dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testInt() throws IOException {
		exchange = new DataExchangeInOutStream();

		final var byteOne = new byte[1];
		for (var pos = 0; pos < 128; pos++) {
			random.nextBytes(byteOne);
			final var number = byteOne[0] & 0xFF;
			exchange.getDestTargetStream().write(number);
			final var result = exchange.getSourceOriginStream().read();
			assertEquals(result, number);
		}
		assertEquals(WORKING, exchange.getState());
	}

	@Test
	void testInt_filtered() throws IOException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new XorTestFilter(0)).addFilter(new XorTestFilter(0));

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

		exchange = new DataExchangeInOutStream();

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				for (var pos = 0; pos < sourceBuffers.size(); pos++) {
					exchange.getDestTargetStream().write(sourceBuffers.get(pos));
				}
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		for (var pos = 0; pos < destBuffers.size(); pos++) {
			read(exchange.getSourceOriginStream(), destBuffers.get(pos));
		}
		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

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
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testBuffers_filtered() throws IOException, InterruptedException, ExecutionException, TimeoutException {
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

		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new XorTestFilter()).addFilter(new XorTestFilter());

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				for (var pos = 0; pos < sourceBuffers.size(); pos++) {
					exchange.getDestTargetStream().write(sourceBuffers.get(pos));
				}
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		for (var pos = 0; pos < destBuffers.size(); pos++) {
			read(exchange.getSourceOriginStream(), destBuffers.get(pos));
		}
		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

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
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
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
			exchange = new DataExchangeInOutStream();
			dataInput = new byte[100];
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
		int bufferSize;

		@BeforeEach
		void init() throws Exception {
			bufferSize = random.nextInt(100) + 100;
			exchange = new DataExchangeInOutStream();
			dataInput = new byte[bufferSize];
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
			assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
		}

		@Test
		void testCloseReadDuringWrite() throws IOException {
			sourceTargetStream.write(dataInput);
			read(destOriginStream, dataOutput);
			destOriginStream.close();
			assertThrows(IOException.class, () -> destOriginStream.read());
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
		}

		@Test
		void testCloseWriteBeforeRead() throws IOException {
			sourceTargetStream.write(dataInput);
			sourceTargetStream.close();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(bufferSize, read(destOriginStream, dataOutput));
			assertEquals(-1, destOriginStream.read());
			assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
		}

		@Test
		void testCloseReadBeforeWrite() throws IOException {
			destOriginStream.close();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
		}

		@Test
		void testCloseTwice() throws IOException {
			destOriginStream.close();
			assertDoesNotThrow(() -> destOriginStream.close());
			sourceTargetStream.close();
			assertDoesNotThrow(() -> sourceTargetStream.close());
			assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
		}
	}

	@Nested
	class StopBeforeEnds {
		byte[] dataInput;
		byte[] dataOutput;
		OutputStream sourceTargetStream;
		InputStream destOriginStream;
		int bufferSize;

		@BeforeEach
		void init() throws Exception {
			bufferSize = random.nextInt(100) + 100;
			exchange = new DataExchangeInOutStream();
			dataInput = new byte[bufferSize];
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
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopReadDuringWrite() throws IOException {
			sourceTargetStream.write(dataInput);
			read(destOriginStream, dataOutput);
			exchange.stop();
			assertEquals(-1, destOriginStream.read());
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopWriteBeforeRead() throws IOException {
			sourceTargetStream.write(dataInput);
			exchange.stop();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(0, read(destOriginStream, dataOutput));
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertThrows(IOException.class, () -> destOriginStream.read());
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopWriteDuringRead2() throws IOException {
			final var dataInput = new byte[bufferSize * 4];

			exchange.stop();
			final var cfWrite = CompletableFuture.runAsync(() -> {
				try {
					sourceTargetStream.write(dataInput);
				} catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			read(destOriginStream, dataOutput);

			Exception e = null;
			try {
				cfWrite.get(1, SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				e = e1;
			}
			assertNotNull(e);
			assertTrue(e instanceof ExecutionException);
			assertTrue(e.getCause() instanceof UncheckedIOException);
			assertTrue(e.getCause().getCause() instanceof IOException);
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopReadBeforeWrite() throws IOException {
			exchange.stop();
			assertThrows(IOException.class, () -> sourceTargetStream.write(dataInput));
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopTwice() throws IOException {
			exchange.stop();
			assertDoesNotThrow(() -> exchange.stop());
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}

		@Test
		void testStopWithClose() throws IOException {
			exchange.stop();
			assertThrows(IOException.class, () -> sourceTargetStream.close());
			assertThrows(IOException.class, () -> destOriginStream.close());
			assertEquals(STOPPED_BY_USER, exchange.getState());
		}
	}

	@Nested
	class StopBeforeEnds_Filtered extends StopBeforeEnds {
		@BeforeEach
		void initFilters() throws Exception {
			exchange.addFilter(new XorTestFilter(0)).addFilter(new XorTestFilter(0));
		}
	}

	@Test
	void testState() throws IOException {
		exchange = new DataExchangeInOutStream();
		assertEquals(State.WORKING, exchange.getState());
		exchange.getSourceOriginStream().close();
		assertEquals(State.WRITER_MANUALLY_CLOSED, exchange.getState());
		exchange.getDestTargetStream().close();
		assertEquals(State.WRITER_MANUALLY_CLOSED, exchange.getState());

		exchange.stop();
		assertEquals(State.WRITER_MANUALLY_CLOSED, exchange.getState());

		exchange = new DataExchangeInOutStream();
		exchange.stop();
		assertEquals(State.STOPPED_BY_USER, exchange.getState());
	}

	@Test
	void testReadAvaliable() throws IOException {
		exchange = new DataExchangeInOutStream();
		final var dataInput = new byte[100];
		final var dataOutput = new byte[dataInput.length];
		final var destOriginStream = exchange.getSourceOriginStream();

		assertEquals(0, destOriginStream.available());
		exchange.getDestTargetStream().write(dataInput);
		assertEquals(100, destOriginStream.available());
		destOriginStream.read();
		assertEquals(99, destOriginStream.available());
		destOriginStream.read(dataOutput);
		assertEquals(0, destOriginStream.available());
		assertEquals(0, destOriginStream.available());

		destOriginStream.close();
		assertEquals(0, destOriginStream.available());

		exchange = new DataExchangeInOutStream();
		destOriginStream.close();
		assertEquals(0, destOriginStream.available());
		assertEquals(WORKING, exchange.getState());

		exchange = new DataExchangeInOutStream();
		exchange.stop();
		assertEquals(0, destOriginStream.available());
		assertEquals(STOPPED_BY_USER, exchange.getState());
	}

	@Test
	void testFilters() {
		exchange = new DataExchangeInOutStream();
		assertEquals(exchange, exchange.addFilter(new XorTestFilter()));
		assertEquals(WORKING, exchange.getState());
	}

	static class XorTestFilter implements DataExchangeFilter {

		final int bufferSize;

		public XorTestFilter() {
			bufferSize = DataExchangeFilter.DEFAULT_BUFFER_SIZE;
		}

		public XorTestFilter(final int bufferSize) {
			this.bufferSize = bufferSize;
		}

		@Override
		public BufferVault applyDataFilter(final boolean last,
		                                   final BufferVault dataSources) throws IOException {
			final var buffer = dataSources.readAll();
			final var result = new byte[buffer.length];
			xor(buffer, result, Byte.MAX_VALUE);
			return BufferVault.wrap(result);
		}

		@Override
		public String getFilterName() {
			return "Internal XorTestFilter";
		}

		@Override
		public int ensureMinDataSourcesDataLength() {
			return bufferSize;
		}
	}

	static void xor(final byte[] in, final byte[] out, final byte value) {
		for (var pos = 0; pos < in.length; pos++) {
			final var byteValue = in[pos] ^ value;
			out[pos] = (byte) byteValue;
		}
	}

	@Test
	void testXorTestFilter() throws IOException {
		final var filter = new XorTestFilter();

		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);

		final var result = filter.applyDataFilter(false, BufferVault.wrap(dataInput));
		assertNotNull(result);
		assertEquals(dataInput.length, result.getSize());
		final var dataOutput = result.readAll();

		assertFalse(Arrays.areEqual(dataOutput, dataInput));
		final var dataAfterXor = new byte[dataInput.length];
		xor(dataOutput, dataAfterXor, Byte.MAX_VALUE);
		assertTrue(Arrays.areEqual(dataAfterXor, dataInput));
	}

	@Test
	void testXorTestFilter_twice() throws IOException {
		final var filter0 = new XorTestFilter();
		final var filter1 = new XorTestFilter();

		final var dataInput0 = new byte[2000];
		random.nextBytes(dataInput0);

		final var result0 = filter0.applyDataFilter(false, BufferVault.wrap(dataInput0));
		final var result1 = filter1.applyDataFilter(false, result0);

		final var dataOutput0 = result0.readAll();
		final var dataOutput1 = result1.readAll();

		assertFalse(Arrays.areEqual(dataOutput0, dataInput0));
		assertFalse(Arrays.areEqual(dataOutput1, dataOutput0));
		assertTrue(Arrays.areEqual(dataOutput1, dataInput0));
	}

	@Test
	void testBaseCopy_StoppedByFilter() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
				return null;
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 1;
			}
		});

		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		ExecutionException writeError = null;
		try {
			writerCF.orTimeout(2, TimeUnit.SECONDS).get();
		} catch (final ExecutionException e) {
			writeError = e;
		}
		assertNotNull(writeError);
		assertTrue(writeError.getCause() instanceof UncheckedIOException);
		assertTrue(writeError.getCause().getCause() instanceof IOException);
		assertEquals("Stopped OutputStream (writer) by filter", writeError.getCause().getCause().getMessage());

		assertEquals(0, copiedSize);
		assertFalse(Arrays.areEqual(dataInput, dataOutput));
		assertTrue(Arrays.areEqual(new byte[dataInput.length], dataOutput));
		assertThrows(IOException.class, () -> exchange.getSourceOriginStream().read());
		assertEquals(STOPPED_BY_FILTER, exchange.getState());
	}

	@Test
	void testBaseCopy_StoppedByFilterAtClose() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
				if (last) {
					return null;
				}
				return new BufferVault();
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 1;
			}
		});

		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		read(exchange.getSourceOriginStream(), dataOutput);

		ExecutionException writeError = null;
		try {
			writerCF.orTimeout(2, TimeUnit.SECONDS).get();
		} catch (final ExecutionException e) {
			writeError = e;
		}
		assertNotNull(writeError);
		assertTrue(writeError.getCause() instanceof UncheckedIOException);
		assertTrue(writeError.getCause().getCause() instanceof IOException);
		assertEquals("Stopped OutputStream (writer) by filter", writeError.getCause().getCause().getMessage());

		// assertEquals(-1, exchange.getSourceOriginStream().read());
		assertEquals(STOPPED_BY_FILTER, exchange.getState());
	}

	@Test
	void testBaseCopy_ErrorWithFilter() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
				throw new IOException("My filter can't process (this is a test)");
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 1;
			}
		});

		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		ExecutionException writeError = null;
		try {
			writerCF.orTimeout(2, TimeUnit.SECONDS).get();
		} catch (final ExecutionException e) {
			writeError = e;
		}
		assertNotNull(writeError);
		assertTrue(writeError.getCause() instanceof UncheckedIOException);
		assertTrue(writeError.getCause().getCause() instanceof IOException);
		assertEquals("Closed OutputStream (writer) caused by filter error",
		        writeError.getCause().getCause().getMessage());

		assertEquals(0, copiedSize);
		assertFalse(Arrays.areEqual(dataInput, dataOutput));
		assertTrue(Arrays.areEqual(new byte[dataInput.length], dataOutput));
		assertEquals(-1, exchange.getSourceOriginStream().read());
		assertEquals(FILTER_ERROR, exchange.getState());
	}

	private List<Integer> filterCancelTransfertItems;

	@BeforeEach
	void init() throws Exception {
		filterCancelTransfertItems = new ArrayList<>();
	}

	abstract class FilterCancelTransfert implements DataExchangeFilter {
		final int ref;

		public FilterCancelTransfert(final int ref) {
			this.ref = ref;
		}

		@Override
		public void onCancelTransfert() {
			filterCancelTransfertItems.add(ref);
		}

		@Override
		public int ensureMinDataSourcesDataLength() {
			return 0;
		}
	}

	@Test
	void testBaseCopy_ErrorWithFilter_CloseAllFilters() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final var buffer = new byte[DataExchangeFilter.DEFAULT_BUFFER_SIZE];
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
				return new BufferVault();
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 0;
			}
		});

		exchange.addFilter(new FilterCancelTransfert(0) {
			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
				assertFalse(last);
				assertEquals(buffer.length, dataSource.getSize());
				throw new IOException("My filter can't process (this is a test)");
			}
		});

		exchange.addFilter(new FilterCancelTransfert(1) {
			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
				return new BufferVault();
			}
		});

		final var countLast = new AtomicInteger();
		final var dataSources = new ArrayList<BufferVault>();

		exchange.addFilter(new FilterCancelTransfert(2) {
			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
				dataSources.add(dataSource);
				if (last) {
					countLast.incrementAndGet();
				}
				return new BufferVault();
			}
		});

		final var anotherIOException = new ArrayList<IOException>();
		exchange.addFilter(new FilterCancelTransfert(3) {
			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
				anotherIOException.add(new IOException("Another error"));
				throw anotherIOException.get(0);
			}
		});

		assertThrows(IOException.class, () -> exchange.getDestTargetStream().write(buffer));
		assertEquals(0, countLast.get());
		assertTrue(dataSources.isEmpty());
		assertTrue(anotherIOException.isEmpty());
		assertEquals(List.of(1, 2, 3), filterCancelTransfertItems);
	}

	@Test
	void testCopyBigger_filtered_transparent() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter((dataSource, last) -> new BufferVault());

		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataInput.length, copiedSize);
		assertTrue(Arrays.areEqual(dataInput, dataOutput));
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testBaseCopy_filtered_multiBuffers() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();

		final var payloads = new String[] { "A123456789", "B123456789",
		                                    "C123456789D123456789E123456789F123456789G123456789" };
		exchange.addFilter((last, dataSource) -> {
			if (last && dataSource.getSize() == 0) {
				return new BufferVault();
			}
			final var result = new BufferVault();
			for (var pos = 0; pos < payloads.length; pos++) {
				result.write(payloads[pos].getBytes());
			}
			return result;
		});

		final var dataInput = "0000".getBytes();
		final var dataOutput = new byte[20];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);

		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		assertEquals(dataOutput.length, copiedSize);
		assertEquals("A123456789B123456789", new String(dataOutput));

		copiedSize = read(exchange.getSourceOriginStream(), dataOutput);
		assertEquals(dataOutput.length, copiedSize);
		assertEquals("C123456789D123456789", new String(dataOutput));

		copiedSize = read(exchange.getSourceOriginStream(), dataOutput);
		assertEquals(dataOutput.length, copiedSize);
		assertEquals("E123456789F123456789", new String(dataOutput));

		copiedSize = read(exchange.getSourceOriginStream(), dataOutput);
		assertEquals(10, copiedSize);
		assertEquals("G123456789", new String(dataOutput, 0, 10));

		copiedSize = read(exchange.getSourceOriginStream(), dataOutput);
		assertEquals(0, copiedSize);
		assertEquals(-1, exchange.getSourceOriginStream().read());
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testCopyBigger_cantCloseFilter() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		exchange = new DataExchangeInOutStream();
		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
				if (last) {
					throw new IOException("Can't close filter (this is a test)");
				}
				return new BufferVault();
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 0;
			}
		});

		final var dataInput = new byte[2000];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		final var copiedSize = read(exchange.getSourceOriginStream(), dataOutput);
		Exception capturedE = null;
		try {
			writerCF.orTimeout(2, TimeUnit.SECONDS).get();
		} catch (final Exception e) {
			capturedE = e;
		}
		assertNotNull(capturedE);
		assertTrue(capturedE instanceof ExecutionException);
		assertTrue(capturedE.getCause() instanceof UncheckedIOException);
		assertTrue(capturedE.getCause().getCause() instanceof IOException);

		assertEquals(2000, copiedSize);
		assertEquals(FILTER_ERROR, exchange.getState());
	}

	@Test
	void testBaseCopy_check_datas_input_filter() throws IOException {
		exchange = new DataExchangeInOutStream();
		final var injectedDatas = Collections.synchronizedList(new ArrayList<BufferVault>());

		exchange.addFilter(new DataExchangeFilter() {

			@Override
			public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
				injectedDatas.add(dataSource.copy());
				return new BufferVault();
			}

			@Override
			public int ensureMinDataSourcesDataLength() {
				return 1;
			}
		});

		final var dataInput = "0123456789".getBytes();
		exchange.getDestTargetStream().write(dataInput);
		exchange.getDestTargetStream().close();

		assertEquals(2, injectedDatas.size());
		final var dataSource = injectedDatas.get(0);
		assertEquals("0123456789", new String(dataSource.readAll()));

		final var endSource = injectedDatas.get(1);
		assertEquals(0, endSource.getSize());
		assertEquals(WRITER_MANUALLY_CLOSED, exchange.getState());
	}

	@Test
	void testEnsureMinBufferSize() throws IOException, InterruptedException, ExecutionException {
		exchange = new DataExchangeInOutStream();
		final var minBufferSize0 = random.nextInt(200) + 100;
		final var minBufferSize1 = random.nextInt(200) + 100;
		exchange.addFilter(new XorTestFilter(minBufferSize0)).addFilter(new XorTestFilter(minBufferSize1));

		final var dataInput = new byte[Math.min(minBufferSize0, minBufferSize1)];
		random.nextBytes(dataInput);
		final var dataOutput = new byte[dataInput.length * 4];

		exchange.getDestTargetStream().write(dataInput);

		final var readerCF = CompletableFuture.runAsync(() -> {
			try {
				read(exchange.getSourceOriginStream(), dataOutput);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}).orTimeout(10, TimeUnit.MILLISECONDS);
		assertThrows(ExecutionException.class, () -> readerCF.get());
	}

	@Test
	void testGetTransfertStats() throws InterruptedException, ExecutionException, IOException {
		exchange = new DataExchangeInOutStream();
		final var filter0 = new XorTestFilter();
		exchange.addFilter(filter0).addFilter(new XorTestFilter());

		final var dataInput = "0123456789".getBytes();
		final var dataOutput = new byte[dataInput.length];

		final var writerCF = CompletableFuture.runAsync(() -> {
			try {
				exchange.getDestTargetStream().write(dataInput);
				assertThrows(IllegalStateException.class, () -> exchange.getTransfertStats(filter0));
				exchange.getDestTargetStream().close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		read(exchange.getSourceOriginStream(), dataOutput);
		writerCF.orTimeout(2, TimeUnit.SECONDS).get();

		final var tStats = exchange.getTransfertStats(filter0);
		assertNotNull(tStats);
		assertTrue(tStats.getDeltaTranfered() >= 0);
		assertTrue(tStats.getTotalDuration() >= 0);
	}

}
